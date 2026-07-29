export function PlaceholderPage({ title, description }) {
  return (
    <main className="system-page container">
      <section className="placeholder-page">
        <span className="section-kicker">UNIWIKI</span>
        <h1>{title}</h1>
        <p>{description}</p>
      </section>
    </main>
  );
}
