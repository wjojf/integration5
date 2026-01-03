"""
Train ML Models Script
Usage: python -m scripts.train_models --dataset /app/data/datasets/v1/game_logs_v1_latest.parquet
"""
import argparse
import sys
sys.path.insert(0, '/app')

from app.modules.ml_models.service import MLService


def main():
    parser = argparse.ArgumentParser(description="Train Connect Four ML models")
    parser.add_argument(
        "--dataset",
        type=str,
        default="/app/data/datasets/v1/game_logs_v1_latest.parquet",
        help="Path to training dataset (parquet file)"
    )
    parser.add_argument(
        "--model",
        type=str,
        choices=["policy", "winprob", "all"],
        default="all",
        help="Which model to train"
    )
    parser.add_argument(
        "--hidden-layers",
        type=str,
        default="128,64,32",
        help="Hidden layer sizes (comma-separated)"
    )
    parser.add_argument(
        "--max-iter",
        type=int,
        default=500,
        help="Maximum training iterations"
    )
    
    args = parser.parse_args()
    
    # Parse hidden layers
    hidden_layers = tuple(int(x) for x in args.hidden_layers.split(","))
    
    print("=" * 50)
    print("Connect Four ML Training (MLP Neural Network)")
    print("=" * 50)
    print(f"Dataset: {args.dataset}")
    print(f"Model: {args.model}")
    print(f"Hidden Layers: {hidden_layers}")
    print(f"Max Iterations: {args.max_iter}")
    print("=" * 50)
    
    # Initialize service
    service = MLService()
    
    if args.model == "policy":
        result = service.train_policy_model(
            args.dataset,
            hidden_layers=hidden_layers,
            max_iter=args.max_iter,
        )
        print("\n--- Policy Model Results ---")
        print(f"Metrics: {result['metrics']}")
        print(f"Model saved to: {result['model_path']}")
        
    elif args.model == "winprob":
        result = service.train_winprob_model(
            args.dataset,
            hidden_layers=hidden_layers,
            max_iter=args.max_iter,
        )
        print("\n--- Win Probability Model Results ---")
        print(f"Metrics: {result['metrics']}")
        print(f"Model saved to: {result['model_path']}")
        
    else:  # all
        results = service.train_all(args.dataset)
        
        print("\n--- Policy Model Results ---")
        print(f"Train Accuracy: {results['policy']['metrics']['train_accuracy']:.2%}")
        print(f"Test Accuracy: {results['policy']['metrics']['test_accuracy']:.2%}")
        print(f"Iterations: {results['policy']['metrics'].get('n_iter', 'N/A')}")
        print(f"Model saved to: {results['policy']['model_path']}")
        
        print("\n--- Win Probability Model Results ---")
        print(f"Train MSE: {results['winprob']['metrics']['train_mse']:.4f}")
        print(f"Test MSE: {results['winprob']['metrics']['test_mse']:.4f}")
        print(f"Test R²: {results['winprob']['metrics']['test_r2']:.4f}")
        print(f"Iterations: {results['winprob']['metrics'].get('n_iter', 'N/A')}")
        print(f"Model saved to: {results['winprob']['model_path']}")
    
    print("\n" + "=" * 50)
    print("Training complete!")
    print("MLFlow: http://localhost:5000")
    print("=" * 50)


if __name__ == "__main__":
    main()
