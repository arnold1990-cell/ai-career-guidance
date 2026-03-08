import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';

const loginSchema = z.object({ email: z.string().email(), password: z.string().min(8) });
const registerSchema = z.object({ fullName: z.string().min(2), email: z.string().email(), password: z.string().min(8), organizationName: z.string().optional() });

export const LoginForm = ({ onSubmit }: { onSubmit: (data: z.infer<typeof loginSchema>) => Promise<void> }) => {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<z.infer<typeof loginSchema>>({ resolver: zodResolver(loginSchema) });
  return (
    <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
      <label className="block text-sm">Email<Input {...register('email')} type="email" /></label>
      {errors.email && <p className="text-xs text-red-600">{errors.email.message}</p>}
      <label className="block text-sm">Password<Input {...register('password')} type="password" /></label>
      {errors.password && <p className="text-xs text-red-600">{errors.password.message}</p>}
      <Button disabled={isSubmitting} type="submit">Sign in</Button>
    </form>
  );
};

export const RegisterForm = ({ type, onSubmit }: { type: 'student' | 'company'; onSubmit: (data: z.infer<typeof registerSchema>) => Promise<void> }) => {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<z.infer<typeof registerSchema>>({ resolver: zodResolver(registerSchema) });
  return (
    <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
      <label className="block text-sm">Full Name<Input {...register('fullName')} /></label>
      <label className="block text-sm">Email<Input {...register('email')} type="email" /></label>
      <label className="block text-sm">Password<Input {...register('password')} type="password" /></label>
      {type === 'company' && <label className="block text-sm">Company Name<Input {...register('organizationName')} /></label>}
      {(errors.fullName || errors.email || errors.password) && <p className="text-xs text-red-600">Please correct the highlighted fields.</p>}
      <Button disabled={isSubmitting} type="submit">Create account</Button>
    </form>
  );
};
