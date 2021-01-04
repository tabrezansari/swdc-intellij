package com.softwareco.intellij.plugin.managers;

import com.intellij.openapi.ui.Messages;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.swdc.snowplow.tracker.events.UIInteractionType;

public class SwitchAccountManager {

    public static void initiateSwitchAccountFlow() {
        // @DialogMessage String message, @DialogTitle String title, @Nullable Icon icon,
        // String[] values, String initialValue, @Nullable InputValidator validator
        String[] options = new String[]{ "Google", "GitHub", "Email" };
        int idx = Messages.showChooseDialog("Switch to a different account?", "Switch account",
                options, options[0], null);
        if (idx >= 0) {
            String authType = options[idx].toLowerCase();
            SoftwareCoSessionManager.launchLogin(authType, UIInteractionType.click, true);
        }
    }

    public static void initiateSignupFlow() {
        // @DialogMessage String message, @DialogTitle String title, @Nullable Icon icon,
        // String[] values, String initialValue, @Nullable InputValidator validator
        String[] options = new String[]{ "Google", "GitHub", "Email" };
        int idx = Messages.showChooseDialog("Sign up using...", "Sign up",
                options, options[0], null);
        if (idx >= 0) {
            String authType = options[idx].toLowerCase();
            SoftwareCoSessionManager.launchLogin(authType, UIInteractionType.click, true);
        }
    }
}
