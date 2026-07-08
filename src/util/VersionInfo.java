package util;

public final class VersionInfo {

    private VersionInfo() {}

    public static String getVersion() {
        String version = VersionInfo.class.getPackage().getImplementationVersion();
        return version != null ? version : "dev";
    }
}
