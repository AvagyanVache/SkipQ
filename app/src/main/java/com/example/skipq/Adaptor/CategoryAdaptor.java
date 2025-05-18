package com.example.skipq.Adaptor;

import android.content.Context;
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
    private int selectedPosition = 0;
    public CategoryAdaptor(Context context, ArrayList<CategoryDomain> categoryDomains, CategoryClickListener categoryClickListener) {
        this.categoryDomains = categoryDomains;
        this.context = context;
        this.categoryClickListener = categoryClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.viewholder_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryDomain category = categoryDomains.get(position);

        holder.categoryName.setText(category.getTitle());
        holder.categoryPic.setImageResource(category.getPic());

        holder.categoryName.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);
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
