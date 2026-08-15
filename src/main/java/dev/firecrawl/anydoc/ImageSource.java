package dev.firecrawl.anydoc;

import java.util.Objects;

/** Where an image's bytes live. */
public sealed interface ImageSource
        permits ImageSource.External, ImageSource.AssetRef, ImageSource.Unavailable {

    /** {@code external}, {@code asset}, or {@code unavailable}. */
    String kind();

    /** Absolute URL with a scheme. */
    record External(String url) implements ImageSource {
        public External {
            Objects.requireNonNull(url, "url");
        }

        @Override
        public String kind() {
            return "external";
        }
    }

    /** Embedded image stored in {@link Document#assets()}. */
    record AssetRef(int assetId) implements ImageSource {
        @Override
        public String kind() {
            return "asset";
        }
    }

    /**
     * No usable source: the image's part is missing or unreadable and it has no URL. Only the alt
     * text remains.
     */
    record Unavailable() implements ImageSource {
        @Override
        public String kind() {
            return "unavailable";
        }
    }
}
