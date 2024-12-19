import React from "react";

interface InputRowProps {
  label: string;
  description?: string;
  children: React.ReactNode;
}

export function InputRow({ label, description, children }: InputRowProps) {
  return (
    <div className="form-row mb-6">
      <h3 className="text-zinc-100 mb-1 text-sm">{label}</h3>
      {children}
      {description && <p className="description text-xs text-zinc-400 mt-2">{description}</p>}
    </div>
  );
}

interface InputFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  name: string;
  label: string;
  description?: string;
  type?: string;
  placeholder?: string;
  className?: string;
}

export function InputField({
  name,
  label,
  description,
  type = "text",
  placeholder = "",
  className = "",
  ...rest
}: InputFieldProps) {
  return (
    <InputRow label={label} description={description}>
      <input
        className={
          "text-lg border bg-zinc-800 border-zinc-600 rounded p-2 outline-none focus:border-blue-500 text-zinc-200 w-full " +
          className
        }
        type={type}
        name={name}
        aria-label={label}
        placeholder={placeholder}
        {...rest}
      />
    </InputRow>
  );
}

export function TextField({ name, label, description, placeholder = "", className = "", ...rest }: InputFieldProps) {
  return (
    <InputField
      type="text"
      name={name}
      label={label}
      description={description}
      placeholder={placeholder}
      className={className}
      {...rest}
    />
  );
}

export function PasswordField({ name, label, description, className = "", ...rest }: InputFieldProps) {
  return (
    <InputField type="password" name={name} label={label} description={description} className={className} {...rest} />
  );
}

export function EmailField({ name, label, description, placeholder = "", className = "", ...rest }: InputFieldProps) {
  return (
    <InputField
      type="email"
      name={name}
      label={label}
      description={description}
      placeholder={placeholder}
      className={className}
      {...rest}
    />
  );
}

interface TextAreaProps extends React.InputHTMLAttributes<HTMLTextAreaElement> {
  name: string;
  label: string;
  description?: string;
  type?: string;
  placeholder?: string;
  className?: string;
}

export function TextAreaField({ name, label, description, placeholder = "", className = "", ...rest }: TextAreaProps) {
  return (
    <InputRow label={label} description={description}>
      <textarea
        className={
          "text-lg border bg-zinc-800 border-zinc-600 rounded p-2 outline-none focus:border-blue-500 text-zinc-200 w-full " +
          className
        }
        name={name}
        aria-label={label}
        placeholder={placeholder}
        {...rest}
      />
    </InputRow>
  );
}

export interface Option {
  name: string;
  label: string;
}

interface ChoiceFieldProps extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, "options"> {
  name: string;
  label: string;
  description?: string;
  options: Option[];
  className?: string;
}

export function ChoiceField({ name, label, description, options, className = "", ...rest }: ChoiceFieldProps) {
  return (
    <InputRow label={label} description={description}>
      <select
        className={
          "text-lg border bg-zinc-800 border-zinc-600 rounded p-2 outline-none focus:border-blue-500 text-zinc-200 w-full " +
          className
        }
        name={name}
        aria-label={label}
        {...rest}
      >
        {options.map((option) => (
          <option key={option.name} value={option.name}>
            {option.label}
          </option>
        ))}
      </select>
    </InputRow>
  );
}

interface SubmitFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  name: string;
  label: string;
  placeholder?: string;
  className?: string;
}

export function SubmitField({ name, label, placeholder = "", className = "", ...rest }: SubmitFieldProps) {
  return (
    <input
      className={
        "border bg-blue-700 hover:bg-zinc-900 border-zinc-700 rounded p-4 text-zinc-100 w-full cursor-pointer " +
        className
      }
      type="submit"
      name={name}
      aria-label={label}
      value={label}
      placeholder={placeholder}
      {...rest}
    />
  );
}
