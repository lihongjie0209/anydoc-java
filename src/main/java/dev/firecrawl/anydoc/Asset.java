package dev.firecrawl.anydoc;

import java.util.Arrays;
import java.util.Objects;

/**
 * An embedded binary asset (image, object payload). Bytes are always retained, so a document stays
 * self-contained.
 */
public final class Asset {

    private final int id;
    private final String mediaType;
    private final String originPart;
    private final byte[] data;

    public Asset(int id, String mediaType, String originPart, byte[] data) {
        this.id = id;
        this.mediaType = Objects.requireNonNull(mediaType, "mediaType");
        this.originPart = Objects.requireNonNull(originPart, "originPart");
        this.data = Objects.requireNonNull(data, "data").clone();
    }

    public int id() {
        return id;
    }

    public String mediaType() {
        return mediaType;
    }

    public String originPart() {
        return originPart;
    }

    public byte[] data() {
        return data.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Asset)) {
            return false;
        }
        Asset asset = (Asset) other;
        return id == asset.id
                && mediaType.equals(asset.mediaType)
                && originPart.equals(asset.originPart)
                && Arrays.equals(data, asset.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, mediaType, originPart, Arrays.hashCode(data));
    }

    @Override
    public String toString() {
        return "Asset[id=" + id + ", mediaType=" + mediaType + ", originPart=" + originPart + "]";
    }
}
