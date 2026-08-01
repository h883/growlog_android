import { Hono, Context } from 'hono';
import { AppContext } from '../types';
import { authMiddleware } from '../middleware/auth';
import { IMAGE_EXTENSIONS, MAX_IMAGE_BYTES, imageUrlFor } from '../media';

const uploads = new Hono<AppContext>();

/** 画像を KV に保存し、`{prefix}/{userId}/{uuid}.{ext}` のキーを返す */
async function storeImage(c: Context<AppContext>, prefix: 'posts' | 'avatars') {
  const firebaseUser = c.get('firebaseUser');

  const contentType = (c.req.header('content-type') ?? '').split(';')[0].trim().toLowerCase();
  const ext = IMAGE_EXTENSIONS[contentType];
  if (!ext) {
    return c.json({ error: 'Unsupported image type' }, 415);
  }

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const body = await c.req.arrayBuffer();
  if (body.byteLength === 0) return c.json({ error: 'Empty body' }, 400);
  if (body.byteLength > MAX_IMAGE_BYTES) {
    return c.json({ error: 'Image too large (max 5MB)' }, 413);
  }

  const key = `${prefix}/${me.user_id}/${crypto.randomUUID()}.${ext}`;
  await c.env.MEDIA.put(key, body, { metadata: { contentType } });

  return c.json({ imageKey: key, imageUrl: imageUrlFor(c.req.url, key) }, 201);
}

uploads.post('/image', authMiddleware, (c) => storeImage(c, 'posts'));
uploads.post('/avatar', authMiddleware, (c) => storeImage(c, 'avatars'));

/**
 * 画像配信。以前は認証なしで、キーに含まれる UUID の推測困難性だけに頼っていたが、
 * URL が漏れると誰でも取得できてしまうため、ログイン必須に変更した。
 * クライアント側は Coil の OkHttp にトークンを付けるインターセプタを入れている。
 */
export const images = new Hono<AppContext>();

// 認証必須になったので共有キャッシュには載せず、端末内だけで持たせる
const CACHE_CONTROL = 'private, max-age=31536000, immutable';

images.get('/:key{.+}', authMiddleware, async (c) => {
  const key = c.req.param('key');
  // キーは UUID を含み中身が差し替わることはないので、キー自体を etag に使える
  const etag = `"${key}"`;

  if (c.req.header('if-none-match') === etag) {
    return new Response(null, { status: 304, headers: { etag, 'cache-control': CACHE_CONTROL } });
  }

  const { value, metadata } = await c.env.MEDIA.getWithMetadata<{ contentType: string }>(
    key,
    { type: 'stream' }
  );
  if (!value) return c.json({ error: 'Not found' }, 404);

  return new Response(value, {
    headers: {
      'content-type': metadata?.contentType ?? 'application/octet-stream',
      'cache-control': CACHE_CONTROL,
      etag,
    },
  });
});

export default uploads;
