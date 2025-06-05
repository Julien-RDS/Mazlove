package com.codename1.demos.grub;

import com.codename1.ui.WebBrowser;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

public class ReactWebView extends Form {
    private static final String INDEX_HTML = 
        "<!DOCTYPE html>" +
        "<html lang=\"fr\">" +
        "<head>" +
        "    <meta charset=\"UTF-8\">" +
        "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
        "    <title>Mazlove</title>" +
        "    <link href=\"https://cdn.jsdelivr.net/npm/tailwindcss@3.3.6/dist/tailwind.min.css\" rel=\"stylesheet\">" +
        "    <script src=\"https://cdn.tailwindcss.com\"></script>" +
        "    <style>" +
        "        .profile-image {" +
        "            background-size: cover;" +
        "            background-position: center;" +
        "        }" +
        "    </style>" +
        "</head>" +
        "<body>" +
        "    <div id=\"root\"></div>" +
        "    <script src=\"https://unpkg.com/react@18.2.0/umd/react.production.min.js\"></script>" +
        "    <script src=\"https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js\"></script>" +
        "    <script src=\"https://unpkg.com/lucide-react@0.300.0/dist/index.umd.js\"></script>" +
        "    <script src=\"../frontend/src/index.js\" type=\"module\"></script>" +
        "</body>" +
        "</html>";

    public ReactWebView() {
        super(new BorderLayout());
        
        // Charger le contenu HTML avec React
        WebBrowser browser = new WebBrowser() {
            @Override
            protected void onLoad() {
                super.onLoad();
                setHTML(INDEX_HTML);
            }

            @Override
            protected void onError(String errorMessage) {
                super.onError(errorMessage);
                // Afficher une erreur plus claire
                setHTML("<div style='text-align:center; padding:20px;'>" +
                        "<h1>Erreur de chargement</h1>" +
                        "<p>" + errorMessage + "</p>" +
                        "</div>");
            }
        };
        
        // Ajouter un indicateur de chargement
        browser.setLoadingHTML("<div style='text-align:center; padding:20px;'>" +
                             "<h1>Chargement...</h1>" +
                             "<div class='animate-spin rounded-full h-8 w-8 border-2 border-white border-t-transparent'></div>" +
                             "</div>");
        
        add(BorderLayout.CENTER, browser);
    }
}
