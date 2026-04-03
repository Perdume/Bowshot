package bowshot.cosmetic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import bowshot.Bowshot;
import bowshot.data.DataFile;
import bowshot.debug.DebugLogger;

public class TitleEffectManager {

    private final Bowshot plugin;
    private final DataFile titleData;

    // 등록된 칭호 목록 (id -> TitleDef)
    private final Map<String, TitleDef> registeredTitles = new LinkedHashMap<>();
    // 등록된 이펙트 목록 (id -> EffectDef)
    private final Map<String, EffectDef> registeredEffects = new LinkedHashMap<>();

    public TitleEffectManager(Bowshot plugin) {
        this.plugin = plugin;
        this.titleData = new DataFile(plugin, "titles.yml");
        createDefaults();
        loadDefinitions();
    }

    // === 기본 칭호/이펙트 생성 (ExcellentCrates 등 외부 플러그인에서 지급 가능) ===

    private void createDefaults() {
        boolean changed = false;

        // 기본 칭호
        String[][] defaultTitles = {
            {"champion", "&6&l[Champion]"},
            {"elite", "&b&l[Elite]"},
            {"legend", "&d&l[Legend]"},
            {"warrior", "&c&l[Warrior]"},
            {"hunter", "&2&l[Hunter]"},
            {"shadow", "&8&l[Shadow]"},
            {"phoenix", "&e&l[Phoenix]"},
            {"frost", "&9&l[Frost]"},
            {"inferno", "&4&l[Inferno]"},
            {"viper", "&a&l[Viper]"},
        };

        for (String[] t : defaultTitles) {
            if (!titleData.getConfig().contains("definitions.titles." + t[0])) {
                titleData.getConfig().set("definitions.titles." + t[0] + ".display", t[1]);
                changed = true;
            }
        }

        // 기본 이펙트
        Object[][] defaultEffects = {
            {"heart", "HEART", "ENTITY_EXPERIENCE_ORB_PICKUP", 10},
            {"flame", "FLAME", "BLAZE_SHOOT", 15},
            {"magic", "ENCHANT", "BLOCK_ENCHANTMENT_TABLE_USE", 20},
            {"smoke", "CAMPFIRE_COSY_SMOKE", "", 12},
            {"star", "END_ROD", "ENTITY_FIREWORK_ROCKET_TWINKLE", 18},
            {"emerald", "HAPPY_VILLAGER", "ENTITY_VILLAGER_YES", 15},
            {"soul", "SOUL_FIRE_FLAME", "PARTICLE_SOUL_ESCAPE", 12},
            {"cherry", "CHERRY_LEAVES", "", 25},
            {"electric", "ELECTRIC_SPARK", "BLOCK_LIGHTNING_ROD_THUNDER", 10},
            {"snow", "SNOWFLAKE", "BLOCK_POWDER_SNOW_STEP", 20},
        };

        for (Object[] e : defaultEffects) {
            String id = (String) e[0];
            if (!titleData.getConfig().contains("definitions.effects." + id)) {
                titleData.getConfig().set("definitions.effects." + id + ".particle", e[1]);
                titleData.getConfig().set("definitions.effects." + id + ".sound", e[2]);
                titleData.getConfig().set("definitions.effects." + id + ".count", e[3]);
                changed = true;
            }
        }

        if (changed) {
            titleData.save();
            DebugLogger.log("Cosmetic", "Default titles/effects created in titles.yml");
        }
    }

    // === 정의 로드 ===

    private void loadDefinitions() {
        registeredTitles.clear();
        registeredEffects.clear();

        ConfigurationSection titleSec = titleData.getConfig().getConfigurationSection("definitions.titles");
        if (titleSec != null) {
            for (String id : titleSec.getKeys(false)) {
                String display = titleSec.getString(id + ".display", "&7[" + id + "]");
                registeredTitles.put(id, new TitleDef(id, display));
            }
        }

        ConfigurationSection effectSec = titleData.getConfig().getConfigurationSection("definitions.effects");
        if (effectSec != null) {
            for (String id : effectSec.getKeys(false)) {
                String particleName = effectSec.getString(id + ".particle", "HEART");
                String soundName = effectSec.getString(id + ".sound", "");
                int count = effectSec.getInt(id + ".count", 10);
                Particle particle;
                try {
                    particle = Particle.valueOf(particleName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    particle = Particle.HEART;
                }
                Sound sound = null;
                if (!soundName.isEmpty()) {
                    try {
                        sound = Sound.valueOf(soundName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // No sound
                    }
                }
                registeredEffects.put(id, new EffectDef(id, particle, sound, count));
            }
        }

        DebugLogger.log("Cosmetic", "Loaded " + registeredTitles.size() + " titles, " + registeredEffects.size() + " effects");
    }

    public void reload() {
        titleData.reload();
        loadDefinitions();
    }

    // === 칭호/이펙트 정의 관리 ===

    public void createTitle(String id, String display) {
        titleData.getConfig().set("definitions.titles." + id + ".display", display);
        titleData.save();
        registeredTitles.put(id, new TitleDef(id, display));
    }

    public void removeTitle(String id) {
        titleData.getConfig().set("definitions.titles." + id, null);
        titleData.save();
        registeredTitles.remove(id);
    }

    public void createEffect(String id, Particle particle, Sound sound, int count) {
        titleData.getConfig().set("definitions.effects." + id + ".particle", particle.name());
        titleData.getConfig().set("definitions.effects." + id + ".sound", sound != null ? sound.name() : "");
        titleData.getConfig().set("definitions.effects." + id + ".count", count);
        titleData.save();
        registeredEffects.put(id, new EffectDef(id, particle, sound, count));
    }

    public void removeEffect(String id) {
        titleData.getConfig().set("definitions.effects." + id, null);
        titleData.save();
        registeredEffects.remove(id);
    }

    public Map<String, TitleDef> getRegisteredTitles() { return registeredTitles; }
    public Map<String, EffectDef> getRegisteredEffects() { return registeredEffects; }

    // === 플레이어 칭호/이펙트 할당 ===

    public void grantTitle(UUID uuid, String titleId) {
        List<String> titles = titleData.getConfig().getStringList("players." + uuid + ".titles");
        if (!titles.contains(titleId)) {
            titles.add(titleId);
            titleData.getConfig().set("players." + uuid + ".titles", titles);
            titleData.save();
        }
    }

    public void revokeTitle(UUID uuid, String titleId) {
        List<String> titles = titleData.getConfig().getStringList("players." + uuid + ".titles");
        titles.remove(titleId);
        titleData.getConfig().set("players." + uuid + ".titles", titles);
        // 장착 중이면 해제
        String equipped = getEquippedTitle(uuid);
        if (titleId.equals(equipped)) {
            titleData.getConfig().set("players." + uuid + ".equipped-title", null);
        }
        titleData.save();
    }

    public void grantEffect(UUID uuid, String effectId) {
        List<String> effects = titleData.getConfig().getStringList("players." + uuid + ".effects");
        if (!effects.contains(effectId)) {
            effects.add(effectId);
            titleData.getConfig().set("players." + uuid + ".effects", effects);
            titleData.save();
        }
    }

    public void revokeEffect(UUID uuid, String effectId) {
        List<String> effects = titleData.getConfig().getStringList("players." + uuid + ".effects");
        effects.remove(effectId);
        titleData.getConfig().set("players." + uuid + ".effects", effects);
        String equipped = getEquippedEffect(uuid);
        if (effectId.equals(equipped)) {
            titleData.getConfig().set("players." + uuid + ".equipped-effect", null);
        }
        titleData.save();
    }

    // === 장착 ===

    public void equipTitle(UUID uuid, String titleId) {
        titleData.getConfig().set("players." + uuid + ".equipped-title", titleId);
        titleData.save();
    }

    public void unequipTitle(UUID uuid) {
        titleData.getConfig().set("players." + uuid + ".equipped-title", null);
        titleData.save();
    }

    public void equipEffect(UUID uuid, String effectId) {
        titleData.getConfig().set("players." + uuid + ".equipped-effect", effectId);
        titleData.save();
    }

    public void unequipEffect(UUID uuid) {
        titleData.getConfig().set("players." + uuid + ".equipped-effect", null);
        titleData.save();
    }

    // === 조회 ===

    public String getEquippedTitle(UUID uuid) {
        return titleData.getConfig().getString("players." + uuid + ".equipped-title", null);
    }

    public String getEquippedEffect(UUID uuid) {
        return titleData.getConfig().getString("players." + uuid + ".equipped-effect", null);
    }

    public List<String> getOwnedTitles(UUID uuid) {
        return titleData.getConfig().getStringList("players." + uuid + ".titles");
    }

    public List<String> getOwnedEffects(UUID uuid) {
        return titleData.getConfig().getStringList("players." + uuid + ".effects");
    }

    /** 장착 중인 칭호의 display 문자열 (없으면 빈 문자열) */
    public String getDisplayPrefix(UUID uuid) {
        String titleId = getEquippedTitle(uuid);
        if (titleId == null) return "";
        TitleDef def = registeredTitles.get(titleId);
        if (def == null) return "";
        return ChatColor.translateAlternateColorCodes('&', def.display()) + " ";
    }

    /** 킬/접속 등에서 이펙트 재생 */
    public void playEffect(Player player) {
        String effectId = getEquippedEffect(player.getUniqueId());
        if (effectId == null) return;
        EffectDef def = registeredEffects.get(effectId);
        if (def == null) return;

        player.getWorld().spawnParticle(def.particle(), player.getLocation().add(0, 1, 0),
                def.count(), 0.5, 0.5, 0.5, 0.02);
        if (def.sound() != null) {
            player.getWorld().playSound(player.getLocation(), def.sound(), 1.0f, 1.0f);
        }
    }

    public DataFile getDataFile() { return titleData; }

    // === Records ===

    public record TitleDef(String id, String display) {}
    public record EffectDef(String id, Particle particle, Sound sound, int count) {}
}
