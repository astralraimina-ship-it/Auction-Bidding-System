package com.auction.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.File;
import java.util.Map;

public class ImageService {
    // ⚠️ Vào trang cloudinary.com đăng ký tài khoản free rồi thay 3 cái chuỗi này vào nhé
    private static final Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dvoz4wgqh",
            "api_key", "582996547526317",
            "api_secret", "cxXICxcTG46C4ezTOulnlprAIdQ"
    ));

    public static String uploadAndGetUrl(File file) {
        if (file == null) return "";
        try {
            // Đẩy file lên Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
            // Lấy đường dẫn URL mà Cloudinary trả về
            return (String) uploadResult.get("url");
        } catch (Exception e) {
            e.printStackTrace();
            return ""; // Nếu lỗi thì trả về chuỗi rỗng
        }
    }
}