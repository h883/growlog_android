import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { Env } from './types';
import authRoutes from './routes/auth';
import userRoutes from './routes/users';
import postRoutes from './routes/posts';
import goalRoutes from './routes/goals';
import commentRoutes from './routes/comments';
import followRoutes from './routes/follows';
import notificationRoutes from './routes/notifications';
import uploadRoutes, { images as imageRoutes } from './routes/uploads';
import discoverRoutes from './routes/discover';
import messageRoutes from './routes/messages';
import groupRoutes from './routes/groups';

const app = new Hono<{ Bindings: Env }>();

app.use(
  '*',
  cors({
    origin: '*',
    allowMethods: ['GET', 'POST', 'PATCH', 'DELETE', 'OPTIONS'],
    allowHeaders: ['Content-Type', 'Authorization'],
  })
);

app.get('/', (c) => c.json({ status: 'ok', service: 'GrowLog API v1' }));

app.route('/api/v1/auth', authRoutes);
app.route('/api/v1/users', userRoutes);
app.route('/api/v1/posts', postRoutes);
app.route('/api/v1/posts', commentRoutes);
app.route('/api/v1/goals', goalRoutes);
app.route('/api/v1/users', followRoutes);
app.route('/api/v1/notifications', notificationRoutes);
app.route('/api/v1/discover', discoverRoutes);
app.route('/api/v1/conversations', messageRoutes);
app.route('/api/v1/groups', groupRoutes);
app.route('/api/v1/uploads', uploadRoutes);
app.route('/api/v1/images', imageRoutes);

app.notFound((c) => c.json({ error: 'Not found' }, 404));

/**
 * Hono に渡す前に平文 HTTP を弾き、返す直前にセキュリティヘッダーを付ける。
 * ミドルウェアではなくここで包んでいるのは、画像配信のように
 * ハンドラが `new Response()` を直接返す経路にも確実にヘッダーを付けるため。
 */
export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);

    // 平文で来たトークンは既に漏れているので、リダイレクトしても取り消せない。
    // それでも以降の通信を HTTPS に固定するために 301 を返す
    if (url.protocol === 'http:') {
      url.protocol = 'https:';
      return Response.redirect(url.toString(), 301);
    }

    const response = await app.fetch(request, env, ctx);

    // 元のレスポンスのヘッダーは書き換え不可のことがあるので、包み直す
    const secured = new Response(response.body, response);
    secured.headers.set(
      'Strict-Transport-Security',
      'max-age=31536000; includeSubDomains; preload'
    );
    secured.headers.set('X-Content-Type-Options', 'nosniff');
    secured.headers.set('Referrer-Policy', 'no-referrer');
    secured.headers.set('X-Frame-Options', 'DENY');
    return secured;
  },
} satisfies ExportedHandler<Env>;
