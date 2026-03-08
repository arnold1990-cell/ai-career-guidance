export const PlaceholderChart = ({ title }: { title: string }) => (
  <div className="card p-4">
    <h3 className="font-semibold">{title}</h3>
    <div className="mt-3 h-40 rounded-lg bg-gradient-to-r from-blue-100 to-emerald-100" />
  </div>
);
