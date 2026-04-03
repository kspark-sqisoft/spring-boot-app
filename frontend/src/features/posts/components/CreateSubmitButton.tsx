import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';

type CreateSubmitButtonProps = {
  pending: boolean;
  label?: string;
};

export function CreateSubmitButton({
  pending,
  label = '등록',
}: CreateSubmitButtonProps) {
  return (
    <Button type="submit" disabled={pending} className="gap-1.5">
      {pending ? (
        <>
          <Loader2 className="size-4 animate-spin" aria-hidden />
          처리 중…
        </>
      ) : (
        label
      )}
    </Button>
  );
}
