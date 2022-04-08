package com.aliucord.plugins;

import static com.aliucord.plugins.ThemeRepoAPI.GITHIB_THEMEREPO_URL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;

import com.aliucord.CollectionUtils;
import com.aliucord.Constants;
import com.aliucord.PluginManager;
import com.aliucord.Utils;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.ConfirmDialog;
import com.aliucord.utils.ChangelogUtils;
import com.discord.app.AppFragment;
import com.discord.widgets.user.usersheet.WidgetUserSheet;
import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.Inflater;

public class ThemesAdapter extends RecyclerView.Adapter<ThemesAdapter.ViewHolder> implements Filterable{
    //com.aliucord.settings.Adapter
    private final AppFragment fragment;
    private final Context ctx;
    public static List<Theme> data;
    public static List<Theme> originalData;

    public ThemesAdapter(AppFragment fragment, Collection<Theme> themes) {
        super();
        this.fragment = fragment;
        ctx = fragment.requireContext();
        originalData = new ArrayList<>(themes);
        data = (List<Theme>) themes;
    }

    public void setData(List<Theme> themes){
        data = themes;
        originalData = new ArrayList<>(themes);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(this, new ThemeCard(ctx));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Theme t = data.get(position);

        if (ThemeRepoAPI.exists(t.name)) {
            holder.card.installButton.setVisibility(View.GONE);
            holder.card.uninstallButton.setVisibility(View.VISIBLE);
        } else {
            holder.card.installButton.setVisibility(View.VISIBLE);
            holder.card.uninstallButton.setVisibility(View.GONE);
        }

        holder.card.descriptionView.setText(t.description);

        String title = String.format("%s v%s by %s", t.name, "", t.author);

        SpannableString spannableTitle = new SpannableString(title);

        int i = title.indexOf(t.author, t.name.length() + 2);
        spannableTitle.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                WidgetUserSheet.Companion.show(t.authorid, fragment.getParentFragmentManager());
            }
        }, i, i + t.author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        holder.card.titleView.setText(spannableTitle);

        holder.card.screenshotsViewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return t.screenshots.size();
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return view == (object);
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                var imageView = new SimpleDraweeView(ctx);
                container.addView(imageView);
                imageView.setImageURI(GITHIB_THEMEREPO_URL + t.screenshots.get(position));
                return imageView;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View)object);
            }
        });

    }

    public void onInstallClick(int position) {
        Theme t = data.get(position);
        Utils.threadPool.execute(() -> {
            if (ThemeRepoAPI.installTheme(t.name)) {
                Utils.mainThread.post(() -> {
                    notifyItemChanged(position);
                });
            }
        });
    }

    public void onUninstallClick(int position) {
        Theme t = data.get(position);
        ConfirmDialog dialog = new ConfirmDialog()
                .setIsDangerous(true)
                .setTitle("Delete " + t.name)
                .setDescription("Are you sure you want to delete this theme? This action cannot be undone.");
        dialog.setOnOkListener(e -> {
            ThemeRepoAPI.deleteTheme(t.name);
            notifyItemChanged(position);
            dialog.dismiss();
            Utils.promptRestart();
        });
        dialog.show(fragment.getParentFragmentManager(), "Confirm Theme Uninstall");

    }
    private final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Theme> resultsList;
            if (constraint == null || constraint.equals(""))
                resultsList = originalData;
            else {
                String search = constraint.toString().toLowerCase().trim();
                resultsList = CollectionUtils.filter(originalData, p -> {
                            if (p.name.toLowerCase().contains(search)) return true;
                            var tags = p.tags;
                            return tags.contains(search);
                        }
                );
            }
            FilterResults results = new FilterResults();
            results.values = resultsList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            @SuppressWarnings("unchecked")
            List<Theme> res = (List<Theme>) results.values;
            var diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return ThemesAdapter.this.getItemCount();
                }
                @Override
                public int getNewListSize() {
                    return res.size();
                }
                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return data.get(oldItemPosition).name.equals(res.get(newItemPosition).name);
                }
                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return true;
                }
            }, false);
            data = res;
            diff.dispatchUpdatesTo(ThemesAdapter.this);
        }
    };

    @Override
    public Filter getFilter() {
        return filter;
    }

    public static final class ViewHolder extends RecyclerView.ViewHolder {
        public final ThemeCard card;
        private final ThemesAdapter adapter;

        @SuppressLint("SetTextI18n")
        public ViewHolder(ThemesAdapter adapter, ThemeCard card) {
            super(card);
            this.adapter = adapter;
            this.card = card;

            card.installButton.setOnClickListener(this::onInstallClick);
            card.uninstallButton.setOnClickListener(this::onUninstallClick);
        }

        public void onUninstallClick(View v) {
            adapter.onUninstallClick(getAdapterPosition());
        }

        public void onInstallClick(View view) {
            adapter.onInstallClick(getAdapterPosition());
        }


    }
}
