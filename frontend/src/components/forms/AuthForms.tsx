import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';

const loginSchema = z.object({
  email: z.string().trim().email('Enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters')
});

const passwordRuleMessage = 'Password must be at least 8 characters and include a number';

const registerSchema = z.object({
  fullName: z
    .string()
    .trim()
    .min(1, 'Full name is required')
    .refine((value) => value.split(/\s+/).length >= 2, 'Full name must include first and last name'),
  email: z.string().trim().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().refine((value) => value.length >= 8 && /\d/.test(value), passwordRuleMessage),
  organizationName: z.string().trim().optional()
});

const inputErrorClass = 'border-red-500 focus:ring-red-500';

type LoginFormValues = z.infer<typeof loginSchema>;
type RegisterFormValues = z.infer<typeof registerSchema>;

export const LoginForm = ({ onSubmit }: { onSubmit: (data: LoginFormValues) => Promise<void> }) => {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) });

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

export const RegisterForm = ({ type, onSubmit }: { type: 'student' | 'company'; onSubmit: (data: RegisterFormValues) => Promise<void> }) => {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    defaultValues: {
      fullName: '',
      email: '',
      password: '',
      organizationName: ''
    }
  });

  return (
    <form className="space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
      <label className="block text-sm">
        Full Name
        <Input className={errors.fullName ? inputErrorClass : ''} autoComplete="name" {...register('fullName')} />
      </label>
      {errors.fullName && <p className="text-xs text-red-600">{errors.fullName.message}</p>}

      <label className="block text-sm">
        Email
        <Input className={errors.email ? inputErrorClass : ''} autoComplete="email" {...register('email')} type="email" />
      </label>
      {errors.email && <p className="text-xs text-red-600">{errors.email.message}</p>}

      <label className="block text-sm">
        Password
        <Input className={errors.password ? inputErrorClass : ''} autoComplete="new-password" {...register('password')} type="password" />
      </label>
      <p className="text-xs text-slate-500">{passwordRuleMessage}.</p>
      {errors.password && <p className="text-xs text-red-600">{errors.password.message}</p>}

      {type === 'company' && <label className="block text-sm">Company Name<Input {...register('organizationName')} /></label>}
      <Button disabled={isSubmitting} type="submit">Create account</Button>
    </form>
  );
};
