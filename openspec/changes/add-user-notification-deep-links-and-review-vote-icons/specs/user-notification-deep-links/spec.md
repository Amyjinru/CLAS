## ADDED Requirements

### Requirement: Reply notifications expose navigation targets
The system SHALL expose structured navigation target metadata for user notifications created from merchant replies or review replies.

#### Scenario: Merchant reply notification contains target data
- **WHEN** a merchant replies to a user's review
- **THEN** the created notification for that user includes a type identifying a merchant review reply and enough target data to navigate to the related review context

#### Scenario: Review reply notification contains target data
- **WHEN** another user replies to a user's review or review reply
- **THEN** the created notification for the notified user includes a type identifying a review reply and enough target data to navigate to the related review context

### Requirement: Notification click opens the target interface
The user notification center SHALL navigate to the corresponding review, order, or merchant interface when a supported reply notification is clicked.

#### Scenario: User clicks merchant reply notification
- **WHEN** a user clicks a merchant reply notification with valid target metadata
- **THEN** the app navigates to the interface showing the corresponding review and merchant reply

#### Scenario: User clicks review reply notification
- **WHEN** a user clicks a review reply notification with valid target metadata
- **THEN** the app navigates to the interface showing the corresponding review thread or reply context

### Requirement: Notification click preserves read handling
The notification center SHALL preserve existing read behavior when users open notification targets.

#### Scenario: Unread target notification is opened
- **WHEN** a user opens an unread supported notification
- **THEN** the notification is marked as read before or during navigation

#### Scenario: Read target notification is opened
- **WHEN** a user opens an already-read supported notification
- **THEN** the app navigates without sending an unnecessary read-state mutation

### Requirement: Unsupported or stale notification targets are safe
The notification center SHALL handle missing, unsupported, deleted, or unauthorized notification targets without crashing or exposing restricted content.

#### Scenario: Notification has no target metadata
- **WHEN** a user clicks a notification that has no target metadata
- **THEN** the app keeps the notification display usable and informs the user that no direct destination is available

#### Scenario: Target content is unavailable
- **WHEN** a user clicks a supported notification whose target content no longer exists or is not accessible
- **THEN** the destination surface shows its normal empty, missing, or permission state without revealing restricted data
