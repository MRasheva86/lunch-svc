package com.lunch.micro.model;

import lombok.Getter;

@Getter
public enum Meal {
    FRIED_CHICKEN_WITH_YOGURT_SOUS ("Fried Chicken with yogurt sous"),
    BAKED_FISH_WITH_VEGETABLES ("Baked fish with vegetables"),
    BEAN_WITH_SALAD ("Bean with salad"),
    BACKED_TURKEY_WITH_PATATOES ("Baked turkey with potatoes"),
    MEAT_BOLLS_WITH_TOMATOES_SOUS ("Meat balls with tomatoes sous");

    private String displayName;
    Meal(String displayName) {
        this.displayName = displayName;
    }
}
