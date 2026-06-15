package pk.gp.pasir_galusza_pawel.dto;

public class DebtNotification {

    private String type;
    private Long groupId;
    private String groupName;
    private String title;
    private Double amount;
    private String message;

    public DebtNotification() {}

    public DebtNotification(String type, Long groupId, String groupName,
                            String title, Double amount, String message) {
        this.type = type;
        this.groupId = groupId;
        this.groupName = groupName;
        this.title = title;
        this.amount = amount;
        this.message = message;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
