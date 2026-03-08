interface Column<T> { key: keyof T; header: string; render?: (item: T) => React.ReactNode; }

export const DataTable = <T extends { id: string }>({ columns, data }: { columns: Column<T>[]; data: T[] }) => (
  <div className="card overflow-x-auto">
    <table className="min-w-full text-sm">
      <thead className="bg-slate-100">
        <tr>{columns.map((col) => <th key={String(col.key)} className="px-4 py-3 text-left font-semibold text-slate-600">{col.header}</th>)}</tr>
      </thead>
      <tbody>
        {data.map((row) => (
          <tr key={row.id} className="border-t border-slate-100">
            {columns.map((col) => <td key={String(col.key)} className="px-4 py-3">{col.render ? col.render(row) : String(row[col.key] ?? '')}</td>)}
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);
