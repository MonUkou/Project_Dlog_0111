package com.example.myapplication.util;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.example.myapplication.R;

/** Ảnh có sẵn trong app để chọn và đăng ngay cả khi thiết bị đang offline. */
public final class PhotoLibrary {
    public static final class Photo {
        @DrawableRes public final int drawableId;
        @NonNull public final String title;
        @NonNull public final String category;
        @NonNull public final String sourceUrl;

        private Photo(@DrawableRes int drawableId, String title, String category, String sourceUrl) {
            this.drawableId = drawableId;
            this.title = title;
            this.category = category;
            this.sourceUrl = sourceUrl;
        }
    }

    private static Photo bundled(int id, String title, String category) {
        return new Photo(id, title, category, "");
    }

    private static Photo pexels(int id, String title, String category, String page) {
        return new Photo(id, title, category, "https://www.pexels.com/photo/" + page + "/");
    }

    private static final Photo[] PHOTOS = {
            bundled(R.drawable.library_photo_01, "Khoảnh khắc 01", "Đời thường"),
            bundled(R.drawable.library_photo_02, "Khoảnh khắc 02", "Đời thường"),
            bundled(R.drawable.library_photo_03, "Khoảnh khắc 03", "Đời thường"),
            bundled(R.drawable.library_photo_04, "Khoảnh khắc 04", "Đời thường"),
            bundled(R.drawable.library_photo_05, "Khoảnh khắc 05", "Đời thường"),
            bundled(R.drawable.library_photo_06, "Khoảnh khắc 06", "Đời thường"),
            bundled(R.drawable.library_photo_07, "Giờ học ở UEH", "Đời thường"),
            bundled(R.drawable.library_photo_08, "Đường phố Việt Nam", "Đời thường"),
            bundled(R.drawable.library_photo_09, "Khoảnh khắc 09", "Đời thường"),
            bundled(R.drawable.library_photo_10, "Khoảnh khắc 10", "Đời thường"),
            bundled(R.drawable.library_photo_11, "Đường phố", "Đời thường"),
            bundled(R.drawable.library_photo_12, "Khoảnh khắc 12", "Đời thường"),
            bundled(R.drawable.library_photo_13, "Khoảnh khắc 13", "Đời thường"),
            bundled(R.drawable.library_photo_14, "Con đường yên tĩnh", "Đời thường"),
            bundled(R.drawable.library_photo_15, "Hang động", "Thiên nhiên"),
            bundled(R.drawable.library_photo_16, "Hà Nội", "Du lịch"),
            bundled(R.drawable.library_photo_17, "Thú cưng", "Đời thường"),
            bundled(R.drawable.library_photo_18, "Món ăn", "Đời thường"),
            bundled(R.drawable.library_photo_19, "Cung đường ven biển", "Du lịch"),
            bundled(R.drawable.library_photo_20, "Khoảnh khắc 20", "Đời thường"),
            bundled(R.drawable.library_photo_21, "Khoảnh khắc 21", "Đời thường"),
            bundled(R.drawable.library_photo_22, "Khoảnh khắc 22", "Đời thường"),
            bundled(R.drawable.library_photo_23, "Tết Việt Nam", "Đời thường"),
            bundled(R.drawable.library_photo_24, "Thành phố Hồ Chí Minh", "Du lịch"),
            bundled(R.drawable.library_photo_25, "UEH", "Đời thường"),
            pexels(R.drawable.library_photo_26, "Hồ và núi", "Thiên nhiên", "forest-and-mountain-near-lake-4122448"),
            pexels(R.drawable.library_photo_27, "Phố đèn lồng Hội An", "Du lịch", "charming-streets-of-hoi-an-with-colorful-lanterns-30091117"),
            pexels(R.drawable.library_photo_28, "Ruộng bậc thang Lào Cai", "Thiên nhiên", "breathtaking-rice-terraces-in-lao-cai-vietnam-33908285"),
            pexels(R.drawable.library_photo_29, "Thuyền cá Phú Quốc", "Du lịch", "aerial-view-of-fishing-boats-in-phu-quoc-harbor-35979695"),
            pexels(R.drawable.library_photo_30, "Chợ đường phố", "Đời thường", "street-market-life-in-vietnam-30372207"),
            pexels(R.drawable.library_photo_31, "Quán cà phê", "Đời thường", "people-at-cafe-in-town-23355067"),
            pexels(R.drawable.library_photo_32, "Làng chài Nha Trang", "Du lịch", "scenic-coastal-fishing-village-overlooking-blue-sea-35468852"),
            pexels(R.drawable.library_photo_33, "Cà phê vỉa hè", "Đời thường", "vietnamese-street-coffee-vendor-at-work-36847799"),
            pexels(R.drawable.library_photo_34, "Hoàng hôn Hạ Long", "Du lịch", "vietnamese-fishing-boats-at-sunset-in-halong-bay-33763986"),
            pexels(R.drawable.library_photo_35, "Người bán hàng Hà Nội", "Đời thường", "street-vendor-at-hanoi-market-scene-30592655"),
            pexels(R.drawable.library_photo_36, "Hồ trong rừng", "Thiên nhiên", "forest-in-mountains-reflecting-in-lake-17180779"),
            pexels(R.drawable.library_photo_37, "Đèn lồng Hội An", "Du lịch", "colorful-street-with-lanterns-in-hoi-an-vietnam-33224215"),
            pexels(R.drawable.library_photo_38, "Bản làng ruộng bậc thang", "Du lịch", "scenic-view-of-rice-terraces-and-mountains-29000300"),
            pexels(R.drawable.library_photo_39, "Xe đạp trên phố", "Đời thường", "elderly-man-walking-bicycle-on-vietnamese-street-36301191"),
            pexels(R.drawable.library_photo_40, "Phố nhiều màu sắc", "Du lịch", "colorful-street-with-vibrant-buildings-in-urban-setting-28727274")
    };

    private PhotoLibrary() { }

    public static int size() {
        return PHOTOS.length;
    }

    @NonNull
    public static Photo get(int position) {
        return PHOTOS[position];
    }

    @NonNull
    public static Uri uri(@NonNull Context context, int position) {
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + get(position).drawableId);
    }
}
