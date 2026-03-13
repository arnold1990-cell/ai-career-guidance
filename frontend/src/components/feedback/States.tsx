export const EmptyState = ({ title, message }: { title: string; message: string }) => (
  <div className="card p-8 text-center">
    <h3 className="text-lg font-semibold">{title}</h3>
    <p className="mt-2 text-sm text-slate-500">{message}</p>
  </div>
);

export const LoadingState = ({ message = 'Loading data...' }: { message?: string }) => <div className="card p-8 text-sm text-slate-500">{message}</div>;
export const ErrorState = ({ message }: { message: string }) => <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">{message}</div>;
