import mlflow
import mlflow.sklearn
from sklearn.linear_model import LogisticRegression
import numpy as np

mlflow.set_tracking_uri("http://localhost:5001")
print("MLflow tracking URI:", mlflow.get_tracking_uri())

try:
    with mlflow.start_run(run_name="test_run"):
        mlflow.log_param("test_param", 1)
        mlflow.log_metric("test_metric", 0.5)
        
        # Create a dummy model
        model = LogisticRegression()
        X = np.array([[1, 2], [3, 4]])
        y = np.array([0, 1])
        model.fit(X, y)
        
        print("Logging model...")
        mlflow.sklearn.log_model(model, "test_model")
        print("Successfully logged to MLFlow")
except Exception as e:
    print(f"MLFlow logging failed: {e}")
    import traceback
    traceback.print_exc()
