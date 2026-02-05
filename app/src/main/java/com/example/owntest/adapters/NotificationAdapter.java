package com.example.owntest.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.owntest.R;
import com.example.owntest.models.Notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notifications;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.notifications = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {

        private ImageView ivIcon;
        private TextView tvTitle, tvMessage, tvTime;
        private View unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);

            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onNotificationClick(notifications.get(position));
                }
            });
        }

        public void bind(Notification notification) {
            // Иконка в зависимости от типа
            if ("TEST_COMPLETED".equals(notification.getType())) {
                ivIcon.setImageResource(R.drawable.ic_notification_test);
                tvTitle.setText("Новое прохождение теста");
                tvMessage.setText(notification.getUserName() + " прошел тест \"" + notification.getTestTitle() + "\"");
            } else if ("TEST_CHECKED".equals(notification.getType())) {
                ivIcon.setImageResource(R.drawable.ic_check);
                tvTitle.setText("Тест проверен");
                tvMessage.setText("Ваш тест \"" + notification.getTestTitle() + "\" проверен");
            }

            // Время
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
            tvTime.setText(sdf.format(new Date(notification.getCreatedDate())));

            // Индикатор непрочитанного
            unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            // Фон для непрочитанных
            if (!notification.isRead()) {
                itemView.setBackgroundResource(R.drawable.bg_notification_unread);
            } else {
                itemView.setBackgroundResource(R.drawable.bg_notification_read);
            }
        }
    }
}