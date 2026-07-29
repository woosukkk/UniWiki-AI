import { Component } from 'react';
import { ErrorMessage } from './ErrorMessage.jsx';

export class AppErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  render() {
    if (this.state.error) {
      return (
        <main className="system-page">
          <ErrorMessage
            title="화면을 표시하지 못했습니다"
            message="잠시 후 다시 시도해 주세요."
            onRetry={() => window.location.reload()}
          />
        </main>
      );
    }
    return this.props.children;
  }
}
