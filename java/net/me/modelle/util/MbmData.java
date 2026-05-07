package net.me.modelle.util;

import java.util.List;

public record MbmData(byte[] textureBytes, List<RawData.BakedVertex> vertices) {}
