/**
 * Interface pour les prestataires de calins à domicile
 */
package com.codename1.demos.grub.interfaces;

import com.codename1.rad.models.Tag;

public interface ICuddleProvider {
    String getName();
    String getDescription();
    String getPicture();
    String getIcon();
    String getCategory();
    double getRating();
    String getMenu();
    String getOrder();
    String getEstimatedDeliveryTime();
    double getDeliveryFee();
    double getDistance();
    double getRestDiscount();

    public static final int CATEGORY_HUGS = 0;
    public static final int CATEGORY_KISSES = 1;
    public static final int CATEGORY_CUDDLES = 2;
    public static final int CATEGORY_ALL = 3;
}
