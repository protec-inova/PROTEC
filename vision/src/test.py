import cv2
from ultralytics import YOLO

model = YOLO("models/best.onnx")

camera = cv2.VideoCapture(1)

while True:
    ret, frame = camera.read()

    if not ret:
        print("Error in cam")
        break

    results = model(frame, conf=0.5, verbose=False)

    for result in results:
        for box in result.boxes:
            class_id = int(box.cls[0])
            confidence = float(box.conf[0])
            class_name = result.names[class_id]

            print(
                f"{class_name}: {confidence:.2f}"
            )

    frame = results[0].plot()

    cv2.imshow("YOLOv8 - Cam", frame)

    if cv2.waitKey(1) & 0xFF == ord("q"):
        break

camera.release()
cv2.destroyAllWindows()