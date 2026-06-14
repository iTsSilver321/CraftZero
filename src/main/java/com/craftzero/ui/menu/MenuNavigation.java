package com.craftzero.ui.menu;

import java.util.Optional;

public interface MenuNavigation {

    void push(Screen screen);

    void replace(Screen screen);

    Optional<Screen> pop();

    void clear();

    boolean back();
}
