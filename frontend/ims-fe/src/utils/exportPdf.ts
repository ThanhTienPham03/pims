import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';

export interface ExportPdfOptions {
  filename?: string;
  orientation?: 'portrait' | 'landscape';
  format?: 'a4';
  marginMm?: number;
}

/**
 * Xuất một phần tử DOM thành file PDF chất lượng cao (chuẩn A4, hỗ trợ tiếng Việt trọn vẹn).
 */
export async function exportElementToPdf(
  element: HTMLElement,
  options: ExportPdfOptions = {}
): Promise<void> {
  const {
    filename = 'document.pdf',
    orientation = 'portrait',
    format = 'a4',
    marginMm = 8,
  } = options;

  const originalBackground = element.style.backgroundColor;
  element.style.backgroundColor = '#ffffff';

  try {
    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false,
      scrollX: 0,
      scrollY: 0,
      windowWidth: Math.max(element.scrollWidth, element.offsetWidth),
    });

    const imgData = canvas.toDataURL('image/jpeg', 0.98);

    const pdf = new jsPDF({
      orientation,
      unit: 'mm',
      format,
    });

    const pageWidth = orientation === 'landscape' ? 297 : 210;
    const pageHeight = orientation === 'landscape' ? 210 : 297;

    const printableWidth = pageWidth - marginMm * 2;
    const printableHeight = pageHeight - marginMm * 2;

    const imgWidth = printableWidth;
    const imgHeight = (canvas.height * printableWidth) / canvas.width;

    if (imgHeight <= printableHeight) {
      pdf.addImage(imgData, 'JPEG', marginMm, marginMm, imgWidth, imgHeight);
    } else {
      let heightLeft = imgHeight;
      let position = marginMm;

      pdf.addImage(imgData, 'JPEG', marginMm, position, imgWidth, imgHeight);
      heightLeft -= printableHeight;

      while (heightLeft > 0) {
        position -= printableHeight;
        pdf.addPage();
        pdf.addImage(imgData, 'JPEG', marginMm, position, imgWidth, imgHeight);
        heightLeft -= printableHeight;
      }
    }

    pdf.save(filename);
  } finally {
    element.style.backgroundColor = originalBackground;
  }
}
