import './Login.css';

const imgAb6AXuDcz5XHxo7VcUuaMvSbNpJp6PR0MzC90FQvChAogRglNeXxlrQDfBTx4R4CFGmXxCz3LzZ4NqfvBaci2CnyBqT9M2NRxmT2Yn7EFgswg3Z8Xu4CQot0El4JGzDfbZrgvMeMkIlolLj3Bb7AkcDiYonB9Baqh9HvCy26RcASvXuWrRhohwH0MOurzdIdM6MhehT8LSzR5YzBftVcEHkwaGsaA3VMbCNr1F5RvjlGhItmaXcnHqZgcg22YlTxXlgIfFrYiOa7Up = "https://www.figma.com/api/mcp/asset/d6181414-2bfb-4118-ab6e-b55b7212a57e";

export default function Login() {
  return (
    <div className="login-page" style={{ backgroundImage: "linear-gradient(90deg, rgb(250, 248, 255) 0%, rgb(250, 248, 255) 100%), linear-gradient(90deg, rgb(255, 255, 255) 0%, rgb(255, 255, 255) 100%)" }}>
      <div className="login-header">
        <div className="logo">YU-BOOK</div>
        <div className="nav-links">
          <div className="nav-link">이용약관</div>
          <div className="nav-link">개인정보처리방침</div>
          <div className="nav-link">고객지원</div>
        </div>
      </div>

      <div className="login-main">
        <div className="background-atmospheric">
          <div className="blur-overlay blur-blue"></div>
          <div className="blur-overlay blur-red"></div>
          <div className="atmospheric-image">
            <img alt="atmospheric" src={imgAb6AXuDcz5XHxo7VcUuaMvSbNpJp6PR0MzC90FQvChAogRglNeXxlrQDfBTx4R4CFGmXxCz3LzZ4NqfvBaci2CnyBqT9M2NRxmT2Yn7EFgswg3Z8Xu4CQot0El4JGzDfbZrgvMeMkIlolLj3Bb7AkcDiYonB9Baqh9HvCy26RcASvXuWrRhohwH0MOurzdIdM6MhehT8LSzR5YzBftVcEHkwaGsaA3VMbCNr1F5RvjlGhItmaXcnHqZgcg22YlTxXlgIfFrYiOa7Up} />
          </div>
        </div>

        <div className="login-container">
          <div className="login-card">
            <div className="login-header-section">
              <div className="secure-label">SECURE login</div>
              <div className="login-title">로그인</div>
            </div>

            <div className="login-form">
              <div className="form-group">
                <label className="form-label">아이디(이메일)</label>
                <input type="email" className="form-input" placeholder="name@example.com" />
              </div>

              <div className="form-group">
                <div className="label-container">
                  <label className="form-label">비밀번호</label>
                  <a href="#" className="forgot-password">비밀번호를 잊으셨나요?</a>
                </div>
                <input type="password" className="form-input" placeholder="••••••••" />
              </div>

              <button className="login-button">본인인증 실행</button>
            </div>

            <div className="login-footer">
              <span className="footer-text">계정이 없으신가요?</span>
              <span className="footer-link">회원가입</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
