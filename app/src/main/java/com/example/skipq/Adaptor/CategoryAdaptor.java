package com.example.skipq.Adaptor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.skipq.Domain.CategoryDomain;
import com.example.skipq.R;

import java.util.ArrayList;

public class CategoryAdaptor extends RecyclerView.Adapter<CategoryAdaptor.ViewHolder> {


    private final ArrayList<CategoryDomain> categoryDomains;

    Context context;
    public CategoryAdaptor(Context context, ArrayList<CategoryDomain> categoryDomains) {
        this.categoryDomains = categoryDomains;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view =inflater.inflate(R.layout.viewholder_category, parent, false);
      //  View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_category, parent, false);
        return new CategoryAdaptor.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryDomain category = categoryDomains.get(position);

        holder.categoryName.setText(categoryDomains.get(position).getTitle());
       // holder.categoryName.setText(category.getTitle());
        holder.categoryPic.setImageResource(categoryDomains.get(position).getPic());
        /*   int drawableResourceId = holder.itemView.getContext().getResources().getIdentifier(
                category.getPic(), "drawable", holder.itemView.getContext().getPackageName()
        );

      */
     /*   Glide.with(holder.itemView.getContext())
                .load(drawableResourceId)
                .into(holder.categoryPic);

      */

      //  holder.mainLayout.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.cat_background));
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
}
