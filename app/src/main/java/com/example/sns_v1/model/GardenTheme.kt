package com.example.sns_v1.model

/**
 * 作業ガーデン。集中の経過をパーセントではなく「段階」で見せる。
 *
 * 段階番号（0〜5）はサーバーが確定させる。ここは表示の文言と絵文字だけを持つ。
 * 段階は 開始 / 15分 / 30分 / 60分 / 90分 / 完了 に対応し、
 * 予定時間を決めている場合はその時間に合わせて等分される。
 */
enum class GardenTheme(
    val value: String,
    val label: String,
    /** 段階0〜5 の絵文字 */
    val emojis: List<String>,
    /** 段階0〜5 の呼び名 */
    val stageNames: List<String>
) {
    PLANT(
        "plant", "ガーデン",
        listOf("🌰", "🌱", "🌿", "🪴", "🌳", "🌸"),
        listOf("種を植えた", "発芽", "若葉", "大きく育つ", "木になった", "花が咲いた")
    ),
    CITY(
        "city", "街づくり",
        listOf("🚧", "🧱", "🏠", "🏘", "🏢", "🌆"),
        listOf("更地を整地", "基礎工事", "建物が建つ", "通りができる", "高層ビル", "街が完成")
    ),
    ROBOT(
        "robot", "ロボット",
        listOf("📐", "🔩", "⚙️", "🔌", "💡", "🤖"),
        listOf("設計開始", "フレーム完成", "モーター搭載", "配線完了", "プログラム起動", "ロボット完成")
    ),
    SPACE(
        "space", "宇宙開発",
        listOf("📋", "🔧", "🛠", "⛽", "🚀", "🌌"),
        listOf("設計図を引く", "部品が届く", "機体を組む", "燃料充填", "発射台へ", "打ち上げ成功")
    ),
    CAMPFIRE(
        "campfire", "焚き火",
        listOf("🪵", "✨", "🕯", "🔥", "🎇", "🏕"),
        listOf("薪を組む", "着火", "小さな炎", "燃え上がる", "熾火が輝く", "満天の焚き火")
    ),
    AQUARIUM(
        "aquarium", "水族館",
        listOf("💧", "🐟", "🪸", "🐠", "🐋", "🐳"),
        listOf("水を張る", "小魚が来た", "水草が育つ", "群れが増える", "大きな魚", "にぎやかな水槽")
    ),
    ADVENTURE(
        "adventure", "冒険",
        listOf("🎒", "🚶", "🌲", "⛰", "🗺", "🏁"),
        listOf("旅の支度", "出発", "森を抜ける", "山を越える", "目的地が見えた", "到達")
    );

    fun emoji(stage: Int) = emojis[stage.coerceIn(0, MAX_STAGE)]

    fun stageName(stage: Int) = stageNames[stage.coerceIn(0, MAX_STAGE)]

    /** 「若葉が育っています」のような、他人に見せる一文 */
    fun stageSentence(stage: Int) = when (this) {
        PLANT -> "${stageName(stage)}が育っています"
        CITY -> "${stageName(stage)}のところです"
        ROBOT -> "${stageName(stage)}まで進んでいます"
        SPACE -> "${stageName(stage)}のところです"
        CAMPFIRE -> "${stageName(stage)}のところです"
        AQUARIUM -> "${stageName(stage)}のところです"
        ADVENTURE -> "${stageName(stage)}のところです"
    }

    companion object {
        const val MAX_STAGE = 5

        fun from(value: String?) = entries.firstOrNull { it.value == value } ?: PLANT
    }
}
