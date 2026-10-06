export default function AuthLayout({ children }) {
  return (
    <div className="auth-page">
      <section className="auth-hero">
        <div className="brand brand-light">
          <img src="/favicon.svg" alt="" width="38" height="38" />
          <div>
            <strong>CampusConnect</strong>
            <span>Issue &amp; complaint desk</span>
          </div>
        </div>
        <div className="auth-hero-copy">
          <h1>Report it once. Track it until it's fixed.</h1>
          <p>
            Broken projector, patchy Wi-Fi, a leaking cooler — raise campus issues in a minute and follow every step
            from <em>open</em> to <em>resolved</em>.
          </p>
          <ul className="auth-points">
            <li>
              <span>1</span>Students report issues with the details that matter
            </li>
            <li>
              <span>2</span>Admins assign them to the right staff member
            </li>
            <li>
              <span>3</span>Everyone sees status, deadlines and replies in one place
            </li>
          </ul>
        </div>
        <p className="auth-foot">Built with React, Spring Boot &amp; MySQL</p>
      </section>
      <section className="auth-panel">{children}</section>
    </div>
  );
}
