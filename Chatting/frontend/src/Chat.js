import React, { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const Chat = () => {
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [username, setUsername] = useState('');
  const [connected, setConnected] = useState(false);
  const [participants, setParticipants] = useState([]);
  const stompClientRef = useRef(null);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const connect = (name) => {
    setUsername(name);
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/public', (message) => {
          const received = JSON.parse(message.body);
          setMessages(prev => [...prev, received]);
          if (received.type === 'JOIN') {
           if (received.participants && received.participants.length > 0) {
           setParticipants(received.participants);
          } else {
           // participants 없으면 sender만 추가
           setParticipants(prev => [...new Set([...prev, received.sender])]);
         }
      } else if (received.type === 'LEAVE') {
            setParticipants(prev => prev.filter(p => p !== received.sender));
          } else if (received.type === 'CHAT') {
            setParticipants(prev => [...new Set([...prev, received.sender])]);
  }
        });
        client.publish({
          destination: '/app/chat.addUser',
          body: JSON.stringify({ sender: name, type: 'JOIN' })
        });
      }
    });
    client.activate();
    stompClientRef.current = client;
  };

  const sendMessage = () => {
    if (!inputMessage.trim() || !stompClientRef.current) return;
    stompClientRef.current.publish({
      destination: '/app/chat.sendMessage',
      body: JSON.stringify({ sender: username, content: inputMessage, type: 'CHAT' })
    });
    setInputMessage('');
  };

  // 입장 화면
  if (!connected) {
    return (
      <div style={{minHeight:'100vh', background:'#0e0e0e', display:'flex', alignItems:'center', justifyContent:'center'}}>
        <div style={{background:'#1a1a1a', borderRadius:'1.5rem', padding:'2.5rem', width:'100%', maxWidth:'400px', border:'1px solid rgba(255,255,255,0.05)'}}>
          <h2 style={{color:'white', fontWeight:'900', fontSize:'1.5rem', marginBottom:'0.5rem'}}>채팅방 입장</h2>
          <p style={{color:'#888', marginBottom:'2rem', fontSize:'0.9rem'}}>고전의 재해석 X1 공동구매</p>
          <p style={{color:'#aaa', marginBottom:'1rem', fontSize:'0.85rem', fontWeight:'bold'}}>테스트 계정 선택</p>
          <div style={{display:'flex', flexDirection:'column', gap:'0.75rem'}}>
            {[{name:'admin1', role:'구매자'}, {name:'admin2', role:'판매자'}, {name:'admin3', role:'관리자'}].map(user => (
              <button key={user.name} onClick={() => connect(user.name)}
                style={{padding:'1rem', borderRadius:'0.75rem', border:'1px solid rgba(0,106,113,0.3)', background:'rgba(0,106,113,0.1)', color:'white', fontWeight:'bold', cursor:'pointer', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
                <span>{user.name}</span>
                <span style={{color:'#1a7a6e', fontSize:'0.8rem'}}>{user.role}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={{minHeight:'100vh', background:'#0e0e0e', display:'flex'}}>
      {/* 참여자 목록 */}
      <div style={{width:'220px', background:'#1a1a1a', borderRight:'1px solid rgba(255,255,255,0.05)', padding:'1.5rem'}}>
        <h3 style={{color:'white', fontWeight:'bold', marginBottom:'1rem', fontSize:'0.9rem'}}>참여자 {participants.length}명</h3>
        {participants.map((p, i) => (
          <div key={i} style={{display:'flex', alignItems:'center', gap:'0.75rem', marginBottom:'0.75rem'}}>
            <div style={{width:'32px', height:'32px', borderRadius:'50%', background:'#1a7a6e', display:'flex', alignItems:'center', justifyContent:'center', color:'white', fontSize:'0.8rem', fontWeight:'bold'}}>
              {p[0].toUpperCase()}
            </div>
            <span style={{color:'#ccc', fontSize:'0.85rem'}}>{p}</span>
            {p === username && <span style={{color:'#1a7a6e', fontSize:'0.7rem'}}>(나)</span>}
          </div>
        ))}
      </div>

      {/* 채팅 영역 */}
      <div style={{flex:1, display:'flex', flexDirection:'column'}}>
        {/* 헤더 */}
        <div style={{background:'#1a1a1a', borderBottom:'1px solid rgba(255,255,255,0.05)', padding:'1rem 1.5rem'}}>
          <h2 style={{color:'white', fontWeight:'bold', margin:0}}>고전의 재해석 X1 공동구매 채팅</h2>
          <p style={{color:'#888', fontSize:'0.8rem', margin:0}}>{participants.length}명 참여 중</p>
        </div>

        {/* 메시지 목록 */}
        <div style={{flex:1, overflowY:'auto', padding:'1.5rem', display:'flex', flexDirection:'column', gap:'1rem'}}>
          {messages.map((msg, i) => {
            if (msg.type === 'JOIN' || msg.type === 'LEAVE') {
              return (
                <div key={i} style={{textAlign:'center'}}>
                  <span style={{color:'#666', fontSize:'0.75rem', background:'#1a1a1a', padding:'0.25rem 0.75rem', borderRadius:'999px'}}>
                    {msg.type === 'JOIN' ? `${msg.sender}님이 입장했습니다.` : `${msg.sender}님이 퇴장했습니다.`}
                  </span>
                </div>
              );
            }
            const isMe = msg.sender === username;
            return (
              <div key={i} style={{display:'flex', justifyContent: isMe ? 'flex-end' : 'flex-start'}}>
                {!isMe && (
                  <div style={{width:'32px', height:'32px', borderRadius:'50%', background:'#1a7a6e', display:'flex', alignItems:'center', justifyContent:'center', color:'white', fontSize:'0.8rem', fontWeight:'bold', marginRight:'0.5rem', flexShrink:0}}>
                    {msg.sender[0].toUpperCase()}
                  </div>
                )}
                <div>
                  {!isMe && <p style={{color:'#888', fontSize:'0.75rem', margin:'0 0 0.25rem 0'}}>{msg.sender}</p>}
                  <div style={{padding:'0.6rem 1rem', borderRadius:'1rem', maxWidth:'300px', fontSize:'0.9rem', background: isMe ? '#1a7a6e' : '#2a2a2a', color:'white'}}>
                    {msg.content}
                  </div>
                </div>
              </div>
            );
          })}
          <div ref={messagesEndRef} />
        </div>

        {/* 입력창 */}
        <div style={{background:'#1a1a1a', borderTop:'1px solid rgba(255,255,255,0.05)', padding:'1rem 1.5rem', display:'flex', gap:'0.75rem'}}>
          <input
            type="text"
            placeholder="메시지를 입력하세요..."
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
            style={{flex:1, background:'#2a2a2a', border:'none', borderRadius:'0.75rem', padding:'0.75rem 1rem', color:'white', fontSize:'0.9rem', outline:'none'}}
          />
          <button onClick={sendMessage}
            style={{padding:'0.75rem 1.5rem', borderRadius:'0.75rem', background:'#1a7a6e', color:'white', fontWeight:'bold', border:'none', cursor:'pointer', fontSize:'0.9rem'}}>
            전송
          </button>
        </div>
      </div>
    </div>
  );
};

export default Chat;