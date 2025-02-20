package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.InputDialog;
import com.aliucord.views.Button;
import com.aliucord.widgets.BottomSheet;
import com.discord.views.CheckedSetting;

public class BottomShit extends BottomSheet {
    SettingsAPI settings;

    public BottomShit(SettingsAPI settings) {
        this.settings = settings;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        var context = requireContext();
        setPadding(20);

        TextView title = new TextView(context, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Header);
        title.setText("ServerReviews");
        title.setTypeface(ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold));
        title.setGravity(Gravity.START);

        Button crashing = new Button(context);
        crashing.setText("Crashing?");
        crashing.setOnClickListener(v -> {
            var dialog = new InputDialog();
            dialog.setOnDialogShownListener(view1 -> {
                dialog.setTitle("WARNING");
                dialog.getBody().setText("If your aliucord is crashing while authorization long click to 'Enter Token Manually' button in settings, it will redirtect you to api for getting token. After you get your token click to 'Enter Token Manually' button again and paste it in there");
                dialog.getInputLayout().setVisibility(View.GONE);
            });

            dialog.show(getParentFragmentManager(), "fart");

        });

        Button authorizate = new Button(context);
        authorizate.setText("Authorize");
        authorizate.setOnClickListener(oc -> {
            Utils.openPageWithProxy(Utils.getAppActivity(), new AuthorazationPage());
        });

        Button enterTokenManually = new Button(context);
        enterTokenManually.setText("Enter OAUTH Token Manually");
        enterTokenManually.setOnClickListener(oc -> {
            var dialog = new InputDialog().setTitle("Enter Token").setDescription("Long Click To Button to get token (discord sometimes ratelimiting api so if youre getting error thats probably why)");
            dialog.setOnOkListener(v -> {
                var token = dialog.getInput();
                if (!token.equals("")) settings.setString("token", token);
                else
                    Toast.makeText(context, "Please Enter Token", Toast.LENGTH_SHORT).show();
            });
            dialog.show(getParentFragmentManager(), "uga");
        });
        enterTokenManually.setOnLongClickListener(v -> {
            Utils.launchUrl(AuthorazationPage.AUTH_URL);
            return true;
        });

        var disableAds = Utils.createCheckedSetting(context, CheckedSetting.ViewType.CHECK, "Disables Ads in Reviews", "");
        disableAds.setChecked(settings.getBool("disableAds", false));

        disableAds.setOnCheckedListener(aBoolean -> {
            settings.setBool("disableAds", aBoolean);
        });

        var disableWarnings = Utils.createCheckedSetting(context, CheckedSetting.ViewType.CHECK, "Disables Warnings in Reviews", "You will still get banned if you do stupit");
        disableWarnings.setChecked(settings.getBool("disableWarnings", false));

        disableWarnings.setOnCheckedListener(aBoolean -> {
            settings.setBool("disableWarnings", aBoolean);
        });

        addView(title);
        addView(crashing);
        addView(authorizate);
        addView(enterTokenManually);
        addView(disableAds);
        addView(disableWarnings);

    }
}
