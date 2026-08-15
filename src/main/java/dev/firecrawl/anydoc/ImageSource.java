package dev.firecrawl.anydoc;

import java.util.Objects;

/** Where an image's bytes live. */
public interface ImageSource {

    /** {@code external}, {@code asset}, or {@code unavailable}. */
    String kind();

    /** Absolute URL with a scheme. */
    final class External implements ImageSource {
        private final String url;

        public External(String url) {
            this.url = Objects.requireNonNull(url, "url");
        }

        public String url() {
            return url;
        }

        @Override
        public String kind() {
            return "external";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof External)) {
                return false;
            }
            return url.equals(((External) other).url);
        }

        @Override
        public int hashCode() {
            return url.hashCode();
        }

        @Override
        public String toString() {
            return "External[url=" + url + "]";
        }
    }

    /** Embedded image stored in {@link Document#assets()}. */
    final class AssetRef implements ImageSource {
        private final int assetId;

        public AssetRef(int assetId) {
            this.assetId = assetId;
        }

        public int assetId() {
            return assetId;
        }

        @Override
        public String kind() {
            return "asset";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof AssetRef)) {
                return false;
            }
            return assetId == ((AssetRef) other).assetId;
        }

        @Override
        public int hashCode() {
            return assetId;
        }

        @Override
        public String toString() {
            return "AssetRef[assetId=" + assetId + "]";
        }
    }

    /**
     * No usable source: the image's part is missing or unreadable and it has no URL. Only the alt
     * text remains.
     */
    final class Unavailable implements ImageSource {
        public Unavailable() {}

        @Override
        public String kind() {
            return "unavailable";
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Unavailable;
        }

        @Override
        public int hashCode() {
            return Unavailable.class.hashCode();
        }

        @Override
        public String toString() {
            return "Unavailable[]";
        }
    }
}
