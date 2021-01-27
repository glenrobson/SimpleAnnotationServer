package uk.org.llgc.annotation.store.data.login;

import java.util.Map;

public class Button {
    protected String _logo = "";
    protected String _text = "";

    public Button(final Map<String, String> pConfig) {
        _logo = pConfig.get("logo");
        _text = pConfig.get("text");
    }

    public String getLogo() {
        return _logo;
    }

    public String getText() {
        return _text;
    }
}
