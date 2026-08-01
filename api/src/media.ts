export const IMAGE_EXTENSIONS: Record<string, string> = {
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
  'image/gif': 'gif',
};

export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

/** KV のキーから、この Worker 経由で配信する公開 URL を組み立てる */
export function imageUrlFor(requestUrl: string, key: string | null | undefined): string | null {
  if (!key) return null;
  return `${new URL(requestUrl).origin}/api/v1/images/${key}`;
}
