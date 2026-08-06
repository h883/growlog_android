/**
 * 作業ガーデン。集中の経過をパーセントではなく「段階」で表す。
 *
 * 段階の名前と絵文字は端末側（Kotlin の GardenTheme）が持つ。
 * サーバーは段階番号だけを確定させ、表示の文言には関与しない。
 */

export const GARDEN_THEMES = [
  'plant',
  'city',
  'robot',
  'space',
  'campfire',
  'aquarium',
  'adventure',
] as const;

export type GardenTheme = (typeof GARDEN_THEMES)[number];

export const DEFAULT_GARDEN_THEME: GardenTheme = 'plant';

export function normalizeTheme(value: string | null | undefined): GardenTheme {
  return GARDEN_THEMES.includes(value as GardenTheme)
    ? (value as GardenTheme)
    : DEFAULT_GARDEN_THEME;
}

/** 段階は 0〜5 の6つ */
export const MAX_GARDEN_STAGE = 5;

/**
 * 予定時間なしのときに使う絶対しきい値（秒）。
 * 仕様の 15 / 30 / 60 / 90 / 120 分に対応する。
 */
const STAGE_ABSOLUTE_SECONDS = [0, 15 * 60, 30 * 60, 60 * 60, 90 * 60, 120 * 60];

/**
 * 予定時間があるときは、その時間を満開（最終段階）として等分する。
 * 30分でも2時間でも同じ割合で進むようにするため（仕様「設定時間に合わせて段階が均等に進む」）。
 * 割合は絶対しきい値を 120 分で割ったもの。
 */
const STAGE_FRACTIONS = STAGE_ABSOLUTE_SECONDS.map((s) => s / STAGE_ABSOLUTE_SECONDS[MAX_GARDEN_STAGE]);

/** 経過秒から到達段階を求める。集中している限り段階は下がらない */
export function gardenStage(
  elapsedSeconds: number,
  plannedDurationSeconds?: number | null
): number {
  const elapsed = Math.max(0, elapsedSeconds);

  const thresholds =
    plannedDurationSeconds && plannedDurationSeconds > 0
      ? STAGE_FRACTIONS.map((f) => f * plannedDurationSeconds)
      : STAGE_ABSOLUTE_SECONDS;

  let stage = 0;
  for (let i = 0; i <= MAX_GARDEN_STAGE; i++) {
    if (elapsed >= thresholds[i]) stage = i;
  }
  return stage;
}

/** 次の段階まであと何秒か。最終段階なら null */
export function secondsToNextStage(
  elapsedSeconds: number,
  plannedDurationSeconds?: number | null
): number | null {
  const stage = gardenStage(elapsedSeconds, plannedDurationSeconds);
  if (stage >= MAX_GARDEN_STAGE) return null;

  const thresholds =
    plannedDurationSeconds && plannedDurationSeconds > 0
      ? STAGE_FRACTIONS.map((f) => f * plannedDurationSeconds)
      : STAGE_ABSOLUTE_SECONDS;

  return Math.max(0, Math.ceil(thresholds[stage + 1] - Math.max(0, elapsedSeconds)));
}
