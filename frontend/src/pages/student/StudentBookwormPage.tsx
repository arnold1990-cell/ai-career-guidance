import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { bookwormService } from '@/services/bookwormService';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { ErrorState } from '@/components/feedback/States';
import type { BookwormChatMessage, BookwormChatResponse } from '@/types';

export const StudentBookwormPage = () => {
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState<BookwormChatMessage[]>([]);
  const [latestResponse, setLatestResponse] = useState<BookwormChatResponse | null>(null);

  const chatMutation = useMutation({
    mutationFn: (message: string) => bookwormService.chat({ message, history: messages }),
    onSuccess: (response, message) => {
      setMessages((prev) => [
        ...prev,
        { role: 'user', content: message },
        { role: 'assistant', content: response.answerText },
      ]);
      setLatestResponse(response);
      setInput('');
    },
  });

  const suggestionsMutation = useMutation({ mutationFn: bookwormService.suggestions });

  const send = () => {
    if (!input.trim() || chatMutation.isPending) return;
    chatMutation.mutate(input.trim());
  };

  const suggestions = suggestionsMutation.data ?? [
    'What can I study with Maths and Physics?',
    'Which universities offer Computer Science?',
    'Show me bursaries for Engineering',
    'Give me a roadmap to become a Data Scientist',
  ];

  return (
    <section className="card p-5 space-y-4">
      <header>
        <h1 className="text-xl font-semibold">Bookworm</h1>
        <p className="text-sm text-slate-600">AI Career Chat Assistance</p>
      </header>

      <div className="flex flex-wrap gap-2">
        {suggestions.map((suggestion) => (
          <button
            key={suggestion}
            type="button"
            className="rounded-full border px-3 py-1 text-xs hover:bg-slate-50"
            onClick={() => setInput(suggestion)}
          >
            {suggestion}
          </button>
        ))}
      </div>

      <div className="max-h-[320px] space-y-2 overflow-y-auto rounded border bg-slate-50 p-3">
        {messages.length === 0 && <p className="text-sm text-slate-500">Ask Bookworm about careers, programmes, universities, or bursaries.</p>}
        {messages.map((message, index) => (
          <div key={`${message.role}-${index}`} className={`max-w-[85%] rounded p-2 text-sm ${message.role === 'user' ? 'ml-auto bg-blue-600 text-white' : 'bg-white text-slate-800 border'}`}>
            {message.content}
          </div>
        ))}
      </div>

      <div className="flex gap-2">
        <Input value={input} onChange={(event) => setInput(event.target.value)} placeholder="Ask Bookworm..." />
        <Button onClick={send} disabled={chatMutation.isPending || !input.trim()}>{chatMutation.isPending ? 'Sending...' : 'Send'}</Button>
      </div>

      {chatMutation.isError && <ErrorState message="Bookworm could not reply right now. Please retry." />}

      {latestResponse && (
        <div className="grid gap-3 md:grid-cols-2">
          <article className="rounded border p-3">
            <h3 className="font-semibold">Recommended universities</h3>
            <div className="mt-2 space-y-2 text-sm">
              {latestResponse.recommendedUniversities.map((university) => (
                <div key={university.name} className="rounded border p-2">
                  <p className="font-medium">{university.name}</p>
                  <p className="text-slate-600">{university.location ?? 'South Africa'}</p>
                  {university.officialWebsite && (
                    <a href={university.officialWebsite} target="_blank" rel="noreferrer" className="text-blue-600 underline">
                      {university.officialWebsite}
                    </a>
                  )}
                </div>
              ))}
            </div>
          </article>

          <article className="rounded border p-3">
            <h3 className="font-semibold">Roadmap</h3>
            <ul className="mt-2 list-disc space-y-1 pl-5 text-sm">
              {latestResponse.roadmapSteps.map((step) => <li key={step}>{step}</li>)}
            </ul>
          </article>
        </div>
      )}
    </section>
  );
};
