package com.example.skipq.Adaptor;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;


import com.example.skipq.Domain.CategoryDomain;
import com.example.skipq.R;

import java.util.ArrayList;
public class CategoryAdaptor extends RecyclerView.Adapter<CategoryAdaptor.ViewHolder> {

    private final ArrayList<CategoryDomain> categoryDomains;
    private final Context context;
    private final CategoryClickListener categoryClickListener;
    private final int itemWidth;
    private int selectedPosition = 0;
    public CategoryAdaptor(Context context, ArrayList<CategoryDomain> categoryDomains, CategoryClickListener categoryClickListener) {
        this.categoryDomains = categoryDomains;
        this.context = context;
        this.categoryClickListener = categoryClickListener;
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        float density = displayMetrics.density;
        int marginBetweenItemsPx = (int) (12 * density); // 12dp margin between items
        int marginEdgePx = (int) (6 * density); // 6dp margin at start and end
        int totalMargins = marginEdgePx * 2 + marginBetweenItemsPx * (categoryDomains.size() - 1); // Start, end, and between items
        int calculatedWidth = (screenWidth - totalMargins) / categoryDomains.size(); // Divide remaining space
        int maxItemWidthPx = (int) (170 * density); // Cap item width at 100dp for tablets
        this.itemWidth = Math.min(calculatedWidth, maxItemWidthPx); // Use smaller of calculated or max width
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.viewholder_category, parent, false);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = itemWidth;
        view.setLayoutParams(params);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryDomain category = categoryDomains.get(position);

        holder.categoryName.setText(category.getTitle());
        holder.categoryPic.setImageResource(category.getPic());

        holder.categoryName.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);
        float density = context.getResources().getDisplayMetrics().density;
        float baseTextSizeSp = 25; // Base text size in sp
        float scaleFactor = itemWidth / (170 * density); // Scale relative to 100dp item width
        float scaledTextSizeSp = baseTextSizeSp * Math.min(scaleFactor, 1.0f); // Cap scaling at 1x for tablets
        holder.categoryName.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledTextSizeSp);

        // Set selected state for visual feedback
        holder.mainLayout.setSelected(position == selectedPosition);

        holder.mainLayout.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            categoryClickListener.onCategoryClick(category.getTitle());
        });
    }

    @Override
    public int getItemCount() {
        return categoryDomains.size();
    }
    public int getItemWidth() {
        return itemWidth;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName;
        ImageView categoryPic;
        ConstraintLayout mainLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.CategoryTextView);
            categoryPic = itemView.findViewById(R.id.CategoryImageView);
            mainLayout = itemView.findViewById(R.id.mainLayout);
        }
    }

    public interface CategoryClickListener {
        void onCategoryClick(String category);
    }
}
