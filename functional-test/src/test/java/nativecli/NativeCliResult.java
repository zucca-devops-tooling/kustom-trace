package nativecli;

public record NativeCliResult(int exitCode, String stdout, String stderr) {}
