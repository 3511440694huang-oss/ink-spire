package com.inkspire.rogue.model;

/**
 * 遗物定义。除了少量需要特殊逻辑的遗物（笔尖、死灵之书、三枚蛋）由引擎按 id 特判外，
 * 其余遗物全部用下面的数值字段描述，引擎在固定时机统一结算——加遗物只需要填数字，不用改逻辑。
 */
public final class RelicDef {
    public enum Hook { NONE, COMBAT_START, TURN_START, CARD_PLAY, ON_KILL, REST, PICKUP, GOLD, REWARD }

    public final String id, name, glyph, description;
    public final Rarity rarity;
    public final Hook hook;

    /* ---- 拾取 ---- */
    public final int maxHp;          // 拾取时最大生命 +N（同时回 N）
    public final int potionSlots;    // 药水栏上限 +N
    public final int shopDiscount;   // 商店价格 -N%

    /* ---- 战斗开始 ---- */
    public final int startBlock;
    public final int firstTurnEnergy;
    public final int startDraw;
    public final int startStrength;
    public final int startDexterity;
    public final int startThorns;
    public final int startPoison;    // 战斗开始时给所有敌人 N 层中毒
    public final int loseHpStart;    // 战斗开始时失去 N 生命

    /* ---- 每回合 ---- */
    public final int energyPerTurn;
    public final int drawPerTurn;
    public final int blockPerTurn;      // 回合结束（敌人行动前）获得 N 格挡
    public final int strengthPerTurn;   // 回合开始获得 N 力量
    public final int dexterityPerTurn;

    /* ---- 出牌 / 击中 ---- */
    public final int attackBonus;    // 每次攻击每段额外 N 伤害
    public final int cardBlock;      // 每打出一张牌获得 N 格挡
    public final int cardPoison;     // 每打出一张攻击牌给目标 N 层中毒

    /* ---- 结算 ---- */
    public final int restHeal;          // 营火休息额外回复 N
    public final int goldPercent;       // 金币 +N%
    public final int postCombatHeal;    // 每场战斗后回复 N
    public final int postCombatHealLow; // 战斗后若生命低于一半，额外回复 N
    public final int eliteCardBonus;    // 精英/首领奖励额外展示 N 张牌

    /* ---- 特殊逻辑（引擎按 id 特判） ---- */
    public final boolean penNib;         // 每 10 张攻击牌后下一张攻击伤害翻倍
    public final boolean freeFirstPower; // 每场战斗第一张能力牌免费
    public final boolean eggAttack;      // 之后获得的攻击牌自动升级
    public final boolean eggSkill;
    public final boolean eggPower;

    private RelicDef(Builder b) {
        this.id = b.id; this.name = b.name; this.glyph = b.glyph;
        this.description = b.description; this.rarity = b.rarity; this.hook = b.hook;
        this.maxHp = b.maxHp; this.potionSlots = b.potionSlots; this.shopDiscount = b.shopDiscount;
        this.startBlock = b.startBlock; this.firstTurnEnergy = b.firstTurnEnergy;
        this.startDraw = b.startDraw; this.startStrength = b.startStrength;
        this.startDexterity = b.startDexterity; this.startThorns = b.startThorns;
        this.startPoison = b.startPoison; this.loseHpStart = b.loseHpStart;
        this.energyPerTurn = b.energyPerTurn; this.drawPerTurn = b.drawPerTurn;
        this.blockPerTurn = b.blockPerTurn; this.strengthPerTurn = b.strengthPerTurn;
        this.dexterityPerTurn = b.dexterityPerTurn;
        this.attackBonus = b.attackBonus; this.cardBlock = b.cardBlock; this.cardPoison = b.cardPoison;
        this.restHeal = b.restHeal; this.goldPercent = b.goldPercent;
        this.postCombatHeal = b.postCombatHeal; this.postCombatHealLow = b.postCombatHealLow;
        this.eliteCardBonus = b.eliteCardBonus;
        this.penNib = b.penNib; this.freeFirstPower = b.freeFirstPower;
        this.eggAttack = b.eggAttack; this.eggSkill = b.eggSkill; this.eggPower = b.eggPower;
    }

    public static Builder of(String id, String name, String glyph, String description, Rarity rarity, Hook hook) {
        return new Builder(id, name, glyph, description, rarity, hook);
    }

    public static final class Builder {
        private final String id, name, glyph, description;
        private final Rarity rarity;
        private final Hook hook;
        private int maxHp, potionSlots, shopDiscount;
        private int startBlock, firstTurnEnergy, startDraw, startStrength, startDexterity, startThorns;
        private int startPoison, loseHpStart;
        private int energyPerTurn, drawPerTurn, blockPerTurn, strengthPerTurn, dexterityPerTurn;
        private int attackBonus, cardBlock, cardPoison;
        private int restHeal, goldPercent, postCombatHeal, postCombatHealLow, eliteCardBonus;
        private boolean penNib, freeFirstPower, eggAttack, eggSkill, eggPower;

        Builder(String id, String name, String glyph, String description, Rarity rarity, Hook hook) {
            this.id = id; this.name = name; this.glyph = glyph;
            this.description = description; this.rarity = rarity; this.hook = hook;
        }

        public Builder maxHp(int v) { maxHp = v; return this; }
        public Builder potionSlots(int v) { potionSlots = v; return this; }
        public Builder shopDiscount(int v) { shopDiscount = v; return this; }
        public Builder startBlock(int v) { startBlock = v; return this; }
        public Builder firstTurnEnergy(int v) { firstTurnEnergy = v; return this; }
        public Builder startDraw(int v) { startDraw = v; return this; }
        public Builder startStrength(int v) { startStrength = v; return this; }
        public Builder startDexterity(int v) { startDexterity = v; return this; }
        public Builder startThorns(int v) { startThorns = v; return this; }
        public Builder startPoison(int v) { startPoison = v; return this; }
        public Builder loseHpStart(int v) { loseHpStart = v; return this; }
        public Builder energyPerTurn(int v) { energyPerTurn = v; return this; }
        public Builder drawPerTurn(int v) { drawPerTurn = v; return this; }
        public Builder blockPerTurn(int v) { blockPerTurn = v; return this; }
        public Builder strengthPerTurn(int v) { strengthPerTurn = v; return this; }
        public Builder dexterityPerTurn(int v) { dexterityPerTurn = v; return this; }
        public Builder attackBonus(int v) { attackBonus = v; return this; }
        public Builder cardBlock(int v) { cardBlock = v; return this; }
        public Builder cardPoison(int v) { cardPoison = v; return this; }
        public Builder restHeal(int v) { restHeal = v; return this; }
        public Builder goldPercent(int v) { goldPercent = v; return this; }
        public Builder postCombatHeal(int v) { postCombatHeal = v; return this; }
        public Builder postCombatHealLow(int v) { postCombatHealLow = v; return this; }
        public Builder eliteCardBonus(int v) { eliteCardBonus = v; return this; }
        public Builder penNib() { penNib = true; return this; }
        public Builder freeFirstPower() { freeFirstPower = true; return this; }
        public Builder eggAttack() { eggAttack = true; return this; }
        public Builder eggSkill() { eggSkill = true; return this; }
        public Builder eggPower() { eggPower = true; return this; }

        public RelicDef build() { return new RelicDef(this); }
    }
}
