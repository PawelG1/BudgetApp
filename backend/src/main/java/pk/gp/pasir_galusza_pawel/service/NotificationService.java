package pk.gp.pasir_galusza_pawel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pk.gp.pasir_galusza_pawel.dto.DebtNotification;
import pk.gp.pasir_galusza_pawel.dto.GroupExpenseNotification;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final GroupNotificationWebSocketHandler webSocketHandler;

    public NotificationService(GroupNotificationWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public void sendGroupExpenseNotification(String recipientEmail, GroupExpenseNotification notification) {
        log.info("[WS] Wysyłanie powiadomienia GROUP_EXPENSE_ADDED do: {} | tytuł: {} | kwota: {}",
                recipientEmail, notification.getTitle(), notification.getAmount());
        webSocketHandler.sendToUser(recipientEmail, notification);
    }

    public void sendDebtNotification(String recipientEmail, DebtNotification notification) {
        log.info("[WS] Wysyłanie powiadomienia {} do: {} | tytuł: {}",
                notification.getType(), recipientEmail, notification.getTitle());
        webSocketHandler.sendToUser(recipientEmail, notification);
    }
}
