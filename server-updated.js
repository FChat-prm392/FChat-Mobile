require('dotenv').config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const { connectDb } = require('./src/config/db');
const { sendPushNotification } = require('./src/utils/fcmService');
const Account = require('./src/models/Account');
const Chat = require('./src/models/Chat'); // Add this to get chat participants
const setupSwagger = require('./src/config/swagger');
const onlineUsersManager = require('./src/utils/onlineUsers');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  },
  pingTimeout: 60000,
  pingInterval: 25000
});

app.use(express.json());
setupSwagger(app);

const accountRoutes = require('./src/routes/accountRoutes');
const chatRoutes = require('./src/routes/chatRoutes');
const messageRoutes = require('./src/routes/messageRoutes');
const friendshipRoutes = require('./src/routes/friendshipRoutes');
const authRoutes = require('./src/routes/authRoutes');
const Message = require('./src/models/Message');

app.use('/api/auth', authRoutes);
app.use('/api/accounts', accountRoutes);
app.use('/api/chats', chatRoutes);
app.use('/api/messages', messageRoutes);
app.use('/api/friendships', friendshipRoutes);

io.on('connection', (socket) => {
  console.log(`🔌 Socket connected: ${socket.id}`);

  socket.on('register-user', async (userId) => {
    console.log(`👤 Registering user: ${userId} with socket: ${socket.id}`);
    const existingSocketId = onlineUsersManager.getSocketId(userId);
    if (existingSocketId && existingSocketId !== socket.id) {
      console.log(`🔄 Removing old socket ${existingSocketId} for user ${userId}`);
      onlineUsersManager.remove(userId);
    }

    onlineUsersManager.add(userId, socket.id);
    io.emit('user-status', { userId, isOnline: true, lastOnline: null });
    
    // Auto-join all user's chat rooms when they connect
    try {
      const userChats = await Chat.find({
        participants: userId
      }).select('_id');
      
      for (const chat of userChats) {
        socket.join(chat._id.toString());
        console.log(`🏠 Auto-joined user ${userId} to chat room ${chat._id}`);
      }
      
      console.log(`✅ User ${userId} registered and joined ${userChats.length} chat rooms`);
    } catch (error) {
      console.error(`❌ Error auto-joining chat rooms for user ${userId}:`, error);
      console.log(`✅ User ${userId} registered (without auto-join due to error)`);
    }
  });

  socket.on('join-room', (chatId) => {
    console.log(`🏠 Socket ${socket.id} manually joining room: ${chatId}`);
    socket.join(chatId);
  });

  // Handle when user joins a new chat (so they get added to the room)
  socket.on('user-joined-chat', (data) => {
    console.log(`🆕 User ${data.userId} joined new chat ${data.chatId}`);
    socket.join(data.chatId);
    console.log(`🏠 Added user to chat room ${data.chatId}`);
  });

  socket.on('send-message', async (messageData) => {
    try {
      console.log(`📤 Sending message to room ${messageData.chatID}`);
      
      // First emit the message to all users in the chat room
      io.to(messageData.chatID).emit('receive-message', messageData);
      console.log(`📡 Broadcasted receive-message to chat room ${messageData.chatID}`);

      // Emit chat list update for real-time last message sync
      const chatListUpdate = {
        chatId: messageData.chatID,
        lastMessage: messageData.text || '',
        senderName: messageData.senderName || 'Unknown User',
        timestamp: messageData.timestamp || new Date().toISOString()
      };
      
      // Get chat participants to emit chat list updates to all participants
      try {
        const chat = await Chat.findById(messageData.chatID).populate('participants');
        if (chat && chat.participants) {
          // Emit to all participants for chat list updates
          chat.participants.forEach(participant => {
            const participantSocketId = onlineUsersManager.getSocketId(participant._id.toString());
            if (participantSocketId) {
              io.to(participantSocketId).emit('chat-list-update', chatListUpdate);
            }
          });
          console.log(`📋 Chat list updates sent to ${chat.participants.length} participants`);
        }
      } catch (chatError) {
        console.error('❌ Error fetching chat for chat list update:', chatError);
      }

      // Get chat participants to find receiver(s) for push notifications
      let receiverIds = [];
      try {
        const chat = await Chat.findById(messageData.chatID).populate('participants');
        if (chat && chat.participants) {
          receiverIds = chat.participants
            .map(p => p._id.toString())
            .filter(id => id !== messageData.senderID); // Exclude sender
          console.log(`📋 Found ${receiverIds.length} receivers for notifications`);
        }
      } catch (chatError) {
        console.error('❌ Error fetching chat participants:', chatError);
        // Fallback to receiverID if provided
        if (messageData.receiverID && messageData.receiverID.trim() !== '') {
          receiverIds = [messageData.receiverID];
        }
      }

      // Send push notifications to offline receivers
      for (const receiverId of receiverIds) {
        const isOnline = onlineUsersManager.has(receiverId);

        if (!isOnline) {
          try {
            const receiver = await Account.findById(receiverId);
            if (receiver?.fcmToken) {
              console.log(`📲 Sending FCM to ${receiverId}`);
              await sendPushNotification(
                receiver.fcmToken,
                `New message from ${messageData.senderName}`,
                messageData.text || 'You have a new message',
                {
                  chatId: messageData.chatID,
                  senderId: messageData.senderID
                }
              );
            }
          } catch (fcmError) {
            console.error(`❌ Error sending FCM to ${receiverId}:`, fcmError);
          }
        }
      }
    } catch (err) {
      console.error('❌ Error handling send-message:', err);
    }
  });

  socket.on('message-sent', (data) => {
    console.log(`📤 Message sent - ID: ${data.messageId}, Chat: ${data.chatId}, Sender: ${data.senderId}`);
    
    // Emit sent status to sender
    socket.emit('message-status-update', {
      messageId: data.messageId,
      status: 'sent',
      timestamp: new Date()
    });
    
    // Emit delivered status to others in the chat room
    socket.to(data.chatId).emit('message-status-update', {
      messageId: data.messageId,
      status: 'delivered',
      timestamp: new Date()
    });
    
    console.log(`✅ Status updates sent for message ${data.messageId}`);
  });

  // Enhanced sync-message-status with better filtering and chat context
  socket.on('sync-message-status', async (data) => {
    console.log(`🔄 Syncing message status for user: ${data.userId}, chat: ${data.chatId || 'all'}`);
    
    try {
      let query = {
        senderID: data.userId,
        $or: [
          { 'deliveredTo.0': { $exists: true } },
          { 'readBy.0': { $exists: true } }
        ]
      };

      // If chatId is provided, filter by specific chat
      if (data.chatId) {
        query.chatID = data.chatId;
        console.log(`🎯 Filtering sync for specific chat: ${data.chatId}`);
      }

      const messages = await Message.find(query)
        .sort({ createAt: -1 })
        .limit(50);
      
      console.log(`📊 Found ${messages.length} messages to sync for user ${data.userId}`);
      
      let syncCount = 0;
      for (const message of messages) {
        let latestStatus = 'sent';
        let latestTimestamp = message.createAt;
        
        if (message.readBy && message.readBy.length > 0) {
          latestStatus = 'read';
          latestTimestamp = message.readBy[message.readBy.length - 1].timestamp;
          console.log(`📖 Message ${message._id} marked as read`);
        } else if (message.deliveredTo && message.deliveredTo.length > 0) {
          latestStatus = 'delivered';
          latestTimestamp = message.deliveredTo[message.deliveredTo.length - 1].timestamp;
          console.log(`📬 Message ${message._id} marked as delivered`);
        }
        
        socket.emit('message-status-update', {
          messageId: message._id.toString(),
          status: latestStatus,
          timestamp: latestTimestamp
        });
        
        syncCount++;
      }
      
      // Send sync completion notification
      socket.emit('status-sync-complete', {
        count: syncCount,
        userId: data.userId,
        chatId: data.chatId || null
      });
      
      console.log(`✅ Sync complete - ${syncCount} messages synced for user ${data.userId}`);
      
    } catch (error) {
      console.error('❌ Error syncing message status:', error);
      socket.emit('sync-error', {
        error: 'Failed to sync message status',
        userId: data.userId
      });
    }
  });

  // Enhanced message-delivered with better logging
  socket.on('message-delivered', async (data) => {
    console.log(`📬 Message delivered - ID: ${data.messageId}, User: ${data.userId}, Chat: ${data.chatId}`);
    
    try {
      const result = await Message.findByIdAndUpdate(
        data.messageId,
        {
          $addToSet: { 
            deliveredTo: { 
              userId: data.userId, 
              timestamp: new Date() 
            }
          }
        },
        { new: true }
      );
      
      if (result) {
        console.log(`✅ Delivered status saved for message ${data.messageId}`);
        
        // Emit to all users in the chat
        io.to(data.chatId).emit('message-status-update', {
          messageId: data.messageId,
          status: 'delivered',
          userId: data.userId,
          timestamp: new Date()
        });
        
        console.log(`📡 Delivered status broadcasted to chat ${data.chatId}`);
      } else {
        console.log(`⚠️ Message ${data.messageId} not found for delivered update`);
      }
    } catch (error) {
      console.error('❌ Error updating delivered status:', error);
    }
  });

  // Enhanced message-read with better logging and broadcasting
  socket.on('message-read', async (data) => {
    console.log(`📖 Message read - ID: ${data.messageId}, User: ${data.userId}, Chat: ${data.chatId}`);
    
    try {
      const result = await Message.findByIdAndUpdate(
        data.messageId,
        {
          $addToSet: { 
            readBy: { 
              userId: data.userId, 
              timestamp: new Date() 
            }
          }
        },
        { new: true }
      );
      
      if (result) {
        console.log(`✅ Read status saved for message ${data.messageId}`);
        
        // Emit to all users in the chat (especially the sender)
        io.to(data.chatId).emit('message-status-update', {
          messageId: data.messageId,
          status: 'read',
          userId: data.userId,
          timestamp: new Date()
        });
        
        console.log(`📡 Read status broadcasted to chat ${data.chatId}`);
        
        // Also emit specifically to the message sender if they're online
        const message = await Message.findById(data.messageId).populate('senderID');
        if (message && message.senderID) {
          const senderSocketId = onlineUsersManager.getSocketId(message.senderID._id.toString());
          if (senderSocketId) {
            io.to(senderSocketId).emit('message-status-update', {
              messageId: data.messageId,
              status: 'read',
              userId: data.userId,
              timestamp: new Date()
            });
            console.log(`📡 Read status sent directly to sender ${message.senderID._id}`);
          }
        }
      } else {
        console.log(`⚠️ Message ${data.messageId} not found for read update`);
      }
    } catch (error) {
      console.error('❌ Error updating read status:', error);
    }
  });

  socket.on('typing-start', (data) => {
    console.log(`⌨️ User ${data.userName} started typing in chat ${data.chatId}`);
    
    // Emit to all users in the chat room (works for both ChatDetailActivity and ChatListActivity)
    socket.to(data.chatId).emit('user-typing', {
      userId: data.userId,
      userName: data.userName,
      isTyping: true
    });
    
    // Also emit typing-start specifically for chat list updates
    socket.to(data.chatId).emit('typing-start', {
      chatId: data.chatId,
      userId: data.userId,
      userName: data.userName
    });
    
    console.log(`📡 Typing start broadcasted to chat room ${data.chatId}`);
  });

  socket.on('typing-stop', (data) => {
    console.log(`⌨️ User ${data.userId} stopped typing in chat ${data.chatId}`);
    
    // Emit to all users in the chat room (works for both ChatDetailActivity and ChatListActivity)
    socket.to(data.chatId).emit('user-typing', {
      userId: data.userId,
      isTyping: false
    });
    
    // Also emit typing-stop specifically for chat list updates
    socket.to(data.chatId).emit('typing-stop', {
      chatId: data.chatId,
      userId: data.userId
    });
    
    console.log(`📡 Typing stop broadcasted to chat room ${data.chatId}`);
  });

  socket.on('user-entered-chat', (data) => {
    console.log(`👤 user-entered-chat: ${data.userId} entered chat ${data.chatId}`);
    socket.to(data.chatId).emit('user-chat-presence', {
      userId: data.userId,
      isInChat: true
    });
  });

  socket.on('user-left-chat', (data) => {
    console.log(`👤 user-left-chat: ${data.userId} left chat ${data.chatId}`);
    socket.to(data.chatId).emit('user-chat-presence', {
      userId: data.userId,
      isInChat: false
    });
  });

  socket.on('typing', ({ chatID, sender }) => {
    socket.to(chatID).emit('typing', { sender });
  });

  socket.on('disconnect', async () => {
    console.log(`🔌 Socket disconnected: ${socket.id}`);
    const userId = onlineUsersManager.removeBySocketId(socket.id);
    if (userId) {
      try {
        const updatedAccount = await Account.findByIdAndUpdate(
          userId,
          { lastOnline: new Date() },
          { new: true }
        );
        io.emit('user-status', {
          userId,
          isOnline: false,
          lastOnline: updatedAccount.lastOnline
        });
        console.log(`✅ User ${userId} marked offline`);
      } catch (err) {
        console.error(`❌ Error updating lastOnline for user ${userId}:`, err);
      }
    }
  });

  socket.on('user-logout', async (userId) => {
    console.log(`👋 User logout: ${userId}`);
    onlineUsersManager.remove(userId);

    try {
      const updatedAccount = await Account.findByIdAndUpdate(
        userId,
        { lastOnline: new Date() },
        { new: true }
      );
      io.emit('user-status', {
        userId,
        isOnline: false,
        lastOnline: updatedAccount.lastOnline
      });
      console.log(`✅ User ${userId} logged out and marked offline`);
    } catch (err) {
      console.error(`❌ Error updating lastOnline for user ${userId}:`, err);
    }
  });
});

const PORT = process.env.PORT || 3000;
const HOST = '0.0.0.0'; 
connectDb()
  .then(() => {
    server.listen(PORT, HOST, () => {
      console.log(`🚀 Server running at http://${HOST}:${PORT}`);
      console.log(`📡 Socket.IO server ready for connections`);
    });
  })
  .catch((err) => {
    console.error('❌ Error starting server:', err);
  });


module.exports = { onlineUsers: onlineUsersManager };
