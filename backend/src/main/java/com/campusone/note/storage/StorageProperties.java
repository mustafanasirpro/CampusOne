package com.campusone.note.storage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String provider = "disabled";
    private int maxUploadSizeMb = 25;
    private int adminMaxUploadsPerDay = 200;
    private int adminMaxStorageMbPerMonth = 5000;
    private int globalUploadStorageCapMb = 8192;
    private Duration downloadUrlTtl = Duration.ofMinutes(10);
    private final R2 r2 = new R2();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = normalize(provider, "disabled");
    }

    public int getMaxUploadSizeMb() {
        return maxUploadSizeMb;
    }

    public void setMaxUploadSizeMb(int maxUploadSizeMb) {
        this.maxUploadSizeMb = maxUploadSizeMb > 0 ? maxUploadSizeMb : 25;
    }

    public int getAdminMaxUploadsPerDay() {
        return adminMaxUploadsPerDay;
    }

    public void setAdminMaxUploadsPerDay(int adminMaxUploadsPerDay) {
        this.adminMaxUploadsPerDay =
                adminMaxUploadsPerDay > 0 ? adminMaxUploadsPerDay : 200;
    }

    public int getAdminMaxStorageMbPerMonth() {
        return adminMaxStorageMbPerMonth;
    }

    public void setAdminMaxStorageMbPerMonth(
            int adminMaxStorageMbPerMonth) {
        this.adminMaxStorageMbPerMonth =
                adminMaxStorageMbPerMonth > 0
                        ? adminMaxStorageMbPerMonth
                        : 5000;
    }

    public int getGlobalUploadStorageCapMb() {
        return globalUploadStorageCapMb;
    }

    public void setGlobalUploadStorageCapMb(int globalUploadStorageCapMb) {
        this.globalUploadStorageCapMb =
                globalUploadStorageCapMb > 0 ? globalUploadStorageCapMb : 8192;
    }

    public Duration getDownloadUrlTtl() {
        return downloadUrlTtl;
    }

    public void setDownloadUrlTtl(Duration downloadUrlTtl) {
        this.downloadUrlTtl = downloadUrlTtl == null || downloadUrlTtl.isNegative()
                || downloadUrlTtl.isZero()
                ? Duration.ofMinutes(10)
                : downloadUrlTtl;
    }

    public R2 getR2() {
        return r2;
    }

    public boolean isR2Requested() {
        return "r2".equalsIgnoreCase(provider);
    }

    public boolean isR2Configured() {
        return isR2Requested()
                && hasText(r2.endpoint)
                && hasText(r2.accessKeyId)
                && hasText(r2.secretAccessKey)
                && hasText(r2.bucket)
                && hasText(r2.region);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public static class R2 {

        private String endpoint = "";
        private String accessKeyId = "";
        private String secretAccessKey = "";
        private String bucket = "";
        private String region = "auto";
        private String publicBaseUrl = "";

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = normalize(endpoint, "");
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = normalize(accessKeyId, "");
        }

        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = normalize(secretAccessKey, "");
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = normalize(bucket, "");
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = normalize(region, "auto");
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = normalize(publicBaseUrl, "");
        }
    }
}
