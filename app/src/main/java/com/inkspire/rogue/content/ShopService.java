package com.inkspire.rogue.content;

import com.inkspire.rogue.core.RunState;
import com.inkspire.rogue.model.CardDef;
import com.inkspire.rogue.model.PotionDef;
import com.inkspire.rogue.model.Rarity;
import com.inkspire.rogue.model.RelicDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** One visit's stock. Prices are visible and deterministic for the visit. */
public final class ShopService {
    public static final class CardOffer {
        public final CardDef card;
        public int price;
        public boolean sold;
        public CardOffer(CardDef c, int p) { card = c; price = p; }
    }
    public static final class RelicOffer {
        public final RelicDef relic;
        public int price;
        public boolean sold;
        public RelicOffer(RelicDef r, int p) { relic = r; price = p; }
    }
    public static final class PotionOffer {
        public final PotionDef potion;
        public int price;
        public boolean sold;
        public PotionOffer(PotionDef p, int price) { potion = p; this.price = price; }
    }

    public final List<CardOffer> cards = new ArrayList<>();
    public final List<RelicOffer> relics = new ArrayList<>();
    public final List<PotionOffer> potions = new ArrayList<>();
    public int removalPrice = 75;
    public boolean removalUsed;

    public static ShopService generate(Random rng, List<String> ownedRelics) {
        return generate(rng, ownedRelics, "iron");
    }

    public static ShopService generate(RunState run) {
        int discount = 0;
        if (run != null) {
            for (String id : run.relics) {
                RelicDef d = RelicLibrary.get(id);
                if (d != null) discount += d.shopDiscount;
            }
            return generate(run.rng, run.relics, run.characterId, discount);
        }
        return generate(new Random(), new ArrayList<String>(), "iron", 0);
    }

    private static ShopService generate(Random rng, List<String> ownedRelics, String characterId) {
        return generate(rng, ownedRelics, characterId, 0);
    }

    private static ShopService generate(Random rng, List<String> ownedRelics, String characterId, int discount) {
        ShopService s = new ShopService();
        for (CardDef c : CardLibrary.shopPool(rng, 5, characterId)) {
            int base = c.rarity == Rarity.COMMON ? 48 : c.rarity == Rarity.UNCOMMON ? 72 : 108;
            s.cards.add(new CardOffer(c, s.discounted(base + rng.nextInt(13), discount)));
        }
        for (int i = 0; i < 2; i++) {
            RelicDef r = RelicLibrary.random(rng, ownedRelics);
            if (r != null && !contains(s.relics, r.id)) {
                s.relics.add(new RelicOffer(r, s.discounted(145 + rng.nextInt(56), discount)));
            }
        }
        for (int i = 0; i < 3; i++) {
            PotionDef p = PotionLibrary.random(rng);
            if (!containsPotion(s.potions, p.id)) {
                s.potions.add(new PotionOffer(p, s.discounted(38 + rng.nextInt(23), discount)));
            }
        }
        s.removalPrice = s.discounted(75, discount);
        return s;
    }

    private int discounted(int price, int discount) {
        if (discount <= 0) return price;
        int pct = Math.min(90, discount);
        return Math.max(1, (int) Math.ceil(price * (100 - pct) / 100.0));
    }

    private static boolean contains(List<RelicOffer> list, String id) {
        for (RelicOffer r : list) if (r.relic.id.equals(id)) return true;
        return false;
    }

    private static boolean containsPotion(List<PotionOffer> list, String id) {
        for (PotionOffer p : list) if (p.potion.id.equals(id)) return true;
        return false;
    }
}
