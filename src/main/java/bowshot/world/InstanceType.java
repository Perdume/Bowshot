package bowshot.world;

public enum InstanceType {
    PLAY("--PLAY--"),
    VISIT("--VISIT--"),
    EDIT("--EDIT--");

    private final String tag;

    InstanceType(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public String createWorldName(String randomSuffix) {
        return "Bowshot" + tag + randomSuffix;
    }
}
