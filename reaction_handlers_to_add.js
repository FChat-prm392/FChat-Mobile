// Add these event handlers to your Socket.IO connection in your backend server.js
// Place them after the existing socket event handlers

  // Reaction handling events
  socket.on('reaction-added', async (data) => {
    console.log(`😊 Reaction added: ${data.userId} reacted with ${data.emoji} to message ${data.messageId} in chat ${data.chatId}`);
    
    try {
      // Broadcast the reaction to all other users in the chat room (excluding sender)
      socket.to(data.chatId).emit('reaction-added', {
        messageId: data.messageId,
        userId: data.userId,
        userName: data.userName,
        emoji: data.emoji,
        timestamp: data.timestamp
      });
      
      console.log(`📡 Reaction-added broadcasted to chat room ${data.chatId}`);
      
      // Optional: Send push notification to offline users about the reaction
      try {
        const chat = await Chat.findById(data.chatId).populate('participants');
        if (chat && chat.participants) {
          const offlineParticipants = chat.participants
            .filter(p => p._id.toString() !== data.userId && !onlineUsersManager.has(p._id.toString()));
          
          for (const participant of offlineParticipants) {
            if (participant.fcmToken) {
              await sendPushNotification(
                participant.fcmToken,
                `${data.userName} reacted to your message`,
                `${data.userName} reacted with ${data.emoji}`,
                {
                  chatId: data.chatId,
                  messageId: data.messageId,
                  type: 'reaction'
                }
              );
            }
          }
        }
      } catch (fcmError) {
        console.error('❌ Error sending FCM for reaction:', fcmError);
      }
      
    } catch (error) {
      console.error('❌ Error handling reaction-added:', error);
    }
  });

  socket.on('reaction-removed', async (data) => {
    console.log(`😔 Reaction removed: ${data.userId} removed ${data.emoji} from message ${data.messageId} in chat ${data.chatId}`);
    
    try {
      // Broadcast the reaction removal to all other users in the chat room (excluding sender)
      socket.to(data.chatId).emit('reaction-removed', {
        messageId: data.messageId,
        userId: data.userId,
        userName: data.userName,
        emoji: data.emoji,
        timestamp: data.timestamp
      });
      
      console.log(`📡 Reaction-removed broadcasted to chat room ${data.chatId}`);
      
    } catch (error) {
      console.error('❌ Error handling reaction-removed:', error);
    }
  });
