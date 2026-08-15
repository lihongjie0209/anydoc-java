package dev.firecrawl.anydoc;

import java.util.Arrays;
import java.util.Objects;

/**
 * An embedded binary asset (image, object payload). Bytes are always retained, so a document stays
 * self-contained.
 */
public record Asset(int id, String mediaType, String originPart, byte[] data) {

    public Asset {
        Objects.requireNonNull(mediaType, "mediaType");
        Objects.requireNonNull(originPart, "originPart");
        Objects.requireNonNull(data, "data");
        data = data.clone();
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Asset asset
                && id == asset.id
                && mediaType.equals(asset.mediaType)
                && originPart.equals(asset.originPart)
                && Arrays.equals(data, asset.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, mediaType, originPart, Arrays.hashCode(data));
    }
}
