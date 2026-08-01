import { Context, Next } from 'hono';
import { importX509, jwtVerify } from 'jose';
import { AppContext, FirebaseUser } from '../types';

const GOOGLE_CERTS_URL =
  'https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com';

export async function verifyFirebaseToken(
  token: string,
  projectId: string
): Promise<FirebaseUser | null> {
  try {
    const [headerB64] = token.split('.');
    const header = JSON.parse(atob(headerB64)) as { kid: string };

    const res = await fetch(GOOGLE_CERTS_URL);
    const certs = (await res.json()) as Record<string, string>;
    const cert = certs[header.kid];
    if (!cert) return null;

    const publicKey = await importX509(cert, 'RS256');

    const { payload } = await jwtVerify(token, publicKey, {
      audience: projectId,
      issuer: `https://securetoken.google.com/${projectId}`,
    });

    return {
      uid: payload.sub!,
      email: payload['email'] as string | undefined,
      name: payload['name'] as string | undefined,
    };
  } catch {
    return null;
  }
}

export async function authMiddleware(c: Context<AppContext>, next: Next) {
  const authorization = c.req.header('Authorization');
  if (!authorization?.startsWith('Bearer ')) {
    return c.json({ error: 'Unauthorized' }, 401);
  }

  const token = authorization.slice(7);
  const user = await verifyFirebaseToken(token, c.env.FIREBASE_PROJECT_ID);

  if (!user) {
    return c.json({ error: 'Invalid or expired token' }, 401);
  }

  c.set('firebaseUser', user);
  await next();
}
